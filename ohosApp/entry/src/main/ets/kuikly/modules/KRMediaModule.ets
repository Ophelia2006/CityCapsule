import { BusinessError } from '@kit.BasicServicesKit';
import { fileIo } from '@kit.CoreFileKit';
import { photoAccessHelper } from '@kit.MediaLibraryKit';
import { camera, cameraPicker } from '@kit.CameraKit';
import { image } from '@kit.ImageKit';
import hilog from '@ohos.hilog';
import {
  KRAny,
  KuiklyRenderBaseModule,
  KuiklyRenderCallback
} from '@kuikly-open/render';

export class KRMediaModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME: string = 'CCMediaModule';
  private static readonly METHOD_PICK_IMAGES: string = 'pickImages';
  private static readonly METHOD_CAPTURE_IMAGE: string = 'captureImage';
  private static readonly METHOD_DELETE_IMAGES: string = 'deleteImages';
  private static readonly METHOD_PREPARE_AVATAR: string = 'prepareAvatar';
  private static readonly METHOD_DELETE_AVATAR: string = 'deleteAvatar';
  private static readonly METHOD_ENSURE_THUMBNAIL: string = 'ensureThumbnail';
  private static readonly METHOD_MEDIA_STATISTICS: string = 'mediaStatistics';
  private static readonly METHOD_CLEAR_THUMBNAILS: string = 'clearThumbnails';
  private static readonly METHOD_CLEANUP_UNREFERENCED: string = 'cleanupUnreferenced';
  private static readonly MAX_IMAGES: number = 9;
  private static readonly MAX_DELETE_BATCH: number = 32;
  private static readonly LOG_DOMAIN: number = 0x0000;
  private static readonly LOG_TAG: string = 'CityCapsuleMedia';
  private pickerInFlight: boolean = false;
  private cameraInFlight: boolean = false;

  syncMode(): boolean {
    return false;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback): KRAny {
    if (method === KRMediaModule.METHOD_PICK_IMAGES) {
      this.pickImages(params, callback);
    } else if (method === KRMediaModule.METHOD_CAPTURE_IMAGE) {
      this.captureImage(callback);
    } else if (method === KRMediaModule.METHOD_DELETE_IMAGES) {
      this.deleteImages(params, callback);
    } else if (method === KRMediaModule.METHOD_PREPARE_AVATAR) {
      this.prepareAvatar(params, callback);
    } else if (method === KRMediaModule.METHOD_DELETE_AVATAR) {
      this.deleteAvatar(params, callback);
    } else if (method === KRMediaModule.METHOD_ENSURE_THUMBNAIL) {
      this.ensureThumbnail(params, callback);
    } else if (method === KRMediaModule.METHOD_MEDIA_STATISTICS) {
      this.mediaStatistics(callback);
    } else if (method === KRMediaModule.METHOD_CLEAR_THUMBNAILS) {
      this.clearThumbnails(callback);
    } else if (method === KRMediaModule.METHOD_CLEANUP_UNREFERENCED) {
      this.cleanupUnreferenced(params, callback);
    } else {
      callback(JSON.stringify({ 'status': 'unsupported', 'message': '当前媒体方法不受支持。', 'paths': [] }));
    }
    return null;
  }

  private pickImages(params: KRAny, callback: KuiklyRenderCallback): void {
    let maxCount: number = 1;
    try {
      const request = JSON.parse(params as string) as Record<string, number>;
      maxCount = Math.max(1, Math.min(KRMediaModule.MAX_IMAGES, request['maxCount'] ?? 1));
    } catch (_) {
      callback(JSON.stringify({ 'status': 'failure', 'message': '照片选择请求无效。', 'paths': [] }));
      return;
    }

    if (this.pickerInFlight || this.cameraInFlight) {
      callback(JSON.stringify({ 'status': 'failure', 'message': '已有照片选择操作正在进行。', 'paths': [] }));
      return;
    }

    const options = new photoAccessHelper.PhotoSelectOptions();
    options.MIMEType = photoAccessHelper.PhotoViewMIMETypes.IMAGE_TYPE;
    options.maxSelectNumber = maxCount;
    const picker = new photoAccessHelper.PhotoViewPicker();
    this.pickerInFlight = true;
    picker.select(options).then((result: photoAccessHelper.PhotoSelectResult): void => {
      this.pickerInFlight = false;
      if (result.photoUris.length === 0) {
        callback(JSON.stringify({ 'status': 'cancelled', 'message': '', 'paths': [] }));
      } else {
        try {
          const paths = this.copyIntoSandbox(result.photoUris);
          callback(JSON.stringify({ 'status': 'success', 'message': '', 'paths': paths }));
        } catch (_) {
          callback(JSON.stringify({
            'status': 'failure',
            'message': '无法复制所选照片，请重试。',
            'paths': []
          }));
        }
      }
    }).catch((error: BusinessError): void => {
      this.pickerInFlight = false;
      this.logFailure('picker', error);
      callback(JSON.stringify({ 'status': 'failure', 'message': '无法打开照片选择器。', 'paths': [] }));
    });
  }

  private copyIntoSandbox(sourceUris: string[]): string[] {
    const context = this.controller?.getUIAbilityContext();
    if (context === null || context === undefined) {
      throw new Error('UIAbilityContext is unavailable.');
    }
    const directory = `${context.filesDir}/images/original`;
    try {
      if (!fileIo.accessSync(directory)) {
        fileIo.mkdirSync(directory, true);
      }
    } catch (error) {
      this.logFailure('prepare', error);
      throw new Error('Failed to prepare the managed image directory.');
    }
    const timestamp = Date.now();
    const createdPaths: string[] = [];
    try {
      sourceUris.forEach((sourceUri: string, index: number): void => {
        const extension = managedImageExtension(sourceUri);
        const target = `${directory}/capsule_${timestamp}_${index}.${extension}`;
        createdPaths.push(target);
        this.copyMediaUri(sourceUri, target);
      });
      return createdPaths.map((path: string): string => `file://${path}`);
    } catch (_) {
      createdPaths.forEach((path: string): void => {
        try {
          if (fileIo.accessSync(path)) {
            fileIo.unlinkSync(path);
          }
        } catch (_) {
        }
      });
      throw new Error('Failed to copy selected images into the app sandbox.');
    }
  }

  private captureImage(callback: KuiklyRenderCallback): void {
    if (this.pickerInFlight || this.cameraInFlight) {
      callback(JSON.stringify({ 'status': 'failure', 'message': '已有照片操作正在进行。', 'paths': [] }));
      return;
    }
    const context = this.controller?.getUIAbilityContext();
    if (context === null || context === undefined) {
      callback(JSON.stringify({ 'status': 'unsupported', 'message': '当前页面不支持拍照。', 'paths': [] }));
      return;
    }
    let targetPath: string;
    try {
      const directory = `${context.filesDir}/images/original`;
      if (!fileIo.accessSync(directory)) {
        fileIo.mkdirSync(directory, true);
      }
      targetPath = `${directory}/camera_${Date.now()}.jpg`;
      const reserved = fileIo.openSync(
        targetPath,
        fileIo.OpenMode.CREATE | fileIo.OpenMode.READ_WRITE
      );
      fileIo.closeSync(reserved);
    } catch (error) {
      this.logFailure('camera_prepare', error as Error);
      callback(JSON.stringify({ 'status': 'failure', 'message': '无法创建拍照目标文件。', 'paths': [] }));
      return;
    }
    const targetUri = `file://${targetPath}`;
    const profile: cameraPicker.PickerProfile = {
      cameraPosition: camera.CameraPosition.CAMERA_POSITION_BACK,
      saveUri: targetUri
    };
    this.cameraInFlight = true;
    cameraPicker.pick(context, [cameraPicker.PickerMediaType.PHOTO], profile)
      .then((result: cameraPicker.PickerResult): void => {
        this.cameraInFlight = false;
        try {
          const completed = this.ensureCapturedImage(
            targetPath,
            targetUri,
            result.resultUri
          );
          if (completed) {
            callback(JSON.stringify({ 'status': 'success', 'message': '', 'paths': [targetUri] }));
            return;
          }
          this.deleteReservedCameraFile(targetPath);
          callback(JSON.stringify({ 'status': 'cancelled', 'message': '', 'paths': [] }));
        } catch (error) {
          this.deleteReservedCameraFile(targetPath);
          this.logFailure('camera_result_copy', error as Error);
          callback(JSON.stringify({
            'status': 'failure',
            'message': '无法保存拍摄的照片，可以改用相册或纯文字记录。',
            'paths': []
          }));
        }
      })
      .catch((error: BusinessError): void => {
        this.cameraInFlight = false;
        this.deleteReservedCameraFile(targetPath);
        this.logFailure('camera_picker', error);
        callback(JSON.stringify({
          'status': 'failure',
          'message': '无法打开系统相机，可以改用相册或纯文字记录。',
          'paths': []
        }));
      });
  }

  private ensureCapturedImage(targetPath: string, targetUri: string, resultUri: string): boolean {
    if (fileIo.accessSync(targetPath) && fileIo.statSync(targetPath).size > 0) {
      return true;
    }
    if (resultUri.length === 0 || resultUri === targetUri) {
      return false;
    }
    // Some HarmonyOS camera providers return a scoped media URI instead of
    // writing PickerProfile.saveUri. Copy that result into the target that was
    // reserved before launching the system camera.
    this.copyMediaUri(resultUri, targetPath);
    return fileIo.accessSync(targetPath) && fileIo.statSync(targetPath).size > 0;
  }

  private deleteReservedCameraFile(path: string): void {
    try {
      if (fileIo.accessSync(path)) {
        fileIo.unlinkSync(path);
      }
    } catch (error) {
      this.logFailure('camera_cleanup', error as Error);
    }
  }

  private copyMediaUri(sourceUri: string, target: string): void {
    let sourceFile: fileIo.File;
    try {
      sourceFile = fileIo.openSync(sourceUri);
    } catch (error) {
      this.logFailure('open', error);
      throw new Error('Failed to open the selected media URI.');
    }
    try {
      // PhotoViewPicker returns a media-provider URI, not a directly copyable path.
      // Opening it first also consumes the scoped read grant provided by the picker.
      fileIo.copyFileSync(sourceFile.fd, target);
    } catch (error) {
      this.logFailure('copy', error);
      throw new Error('Failed to copy the selected media file.');
    } finally {
      try {
        fileIo.closeSync(sourceFile);
      } catch (error) {
        this.logFailure('close', error);
      }
    }
  }

  private logFailure(stage: string, error: Error | BusinessError): void {
    const code = (error as BusinessError).code ?? -1;
    const message = error.message ?? 'unknown';
    hilog.error(
      KRMediaModule.LOG_DOMAIN,
      KRMediaModule.LOG_TAG,
      'Media operation failed at %{public}s. code=%{public}d message=%{public}s',
      stage,
      code,
      message
    );
  }

  private deleteImages(params: KRAny, callback: KuiklyRenderCallback): void {
    const context = this.controller?.getUIAbilityContext();
    if (context === null || context === undefined) {
      callback(JSON.stringify({ 'status': 'unsupported', 'message': '当前页面不支持照片文件清理。', 'paths': [] }));
      return;
    }
    let paths: string[] = [];
    try {
      const request = JSON.parse(params as string) as MediaDeleteRequest;
      paths = request.paths.slice(0, KRMediaModule.MAX_DELETE_BATCH);
    } catch (_) {
      callback(JSON.stringify({ 'status': 'failure', 'message': '照片清理请求无效。', 'paths': [] }));
      return;
    }
    const directory = `${context.filesDir}/images/original`;
    const prefix = `file://${directory}/`;
    if (paths.some((path: string): boolean => !this.isManagedPath(path, prefix))) {
      callback(JSON.stringify({
        'status': 'failure',
        'message': '照片路径不属于应用管理目录，未执行删除。',
        'paths': []
      }));
      return;
    }
    const deleted: string[] = [];
    try {
      paths.forEach((path: string): void => {
        const filePath = path.substring('file://'.length);
        if (fileIo.accessSync(filePath)) {
          fileIo.unlinkSync(filePath);
        }
        const fileName = filePath.substring(directory.length + 1);
        const thumbnail = this.thumbnailPath(directory, fileName);
        const legacyThumbnail = this.legacyThumbnailPath(directory, fileName);
        if (fileIo.accessSync(thumbnail)) fileIo.unlinkSync(thumbnail);
        if (fileIo.accessSync(legacyThumbnail)) fileIo.unlinkSync(legacyThumbnail);
        deleted.push(path);
      });
      callback(JSON.stringify({ 'status': 'success', 'message': '', 'paths': deleted }));
    } catch (_) {
      callback(JSON.stringify({ 'status': 'failure', 'message': '照片文件暂时无法清理。', 'paths': deleted }));
    }
  }

  private ensureThumbnail(params: KRAny, callback: KuiklyRenderCallback): void {
    const context = this.controller?.getUIAbilityContext();
    if (context === null || context === undefined) return callback(JSON.stringify({ 'status': 'unsupported' }));
    let originalUri = '';
    try { originalUri = (JSON.parse(params as string) as MediaThumbnailRequest).path; } catch (_) {}
    const originalDir = `${context.filesDir}/images/original`;
    const prefix = `file://${originalDir}/`;
    if (!this.isManagedPath(originalUri, prefix)) return callback(JSON.stringify({ 'status': 'failure', 'message': '原图路径无效。' }));
    const original = originalUri.substring('file://'.length);
    const target = this.thumbnailPath(originalDir, original.substring(originalDir.length + 1));
    try {
      const thumbnailDir = `${context.filesDir}/images/thumbnail`;
      if (!fileIo.accessSync(thumbnailDir)) fileIo.mkdirSync(thumbnailDir, true);
      if (fileIo.accessSync(target) && fileIo.statSync(target).size > 0) {
        callback(JSON.stringify({ 'status': 'success', 'path': `file://${target}` }));
        return;
      }
      const source = image.createImageSource(original);
      source.getImageInfo().then((info: image.ImageInfo): void => {
        const longestEdge = Math.max(info.size.width, info.size.height);
        if (longestEdge <= 0) {
          source.release();
          callback(JSON.stringify({ 'status': 'failure', 'message': '原图尺寸无效。' }));
          return;
        }
        const scale = Math.min(1, 512 / longestEdge);
        const targetWidth = Math.max(1, Math.round(info.size.width * scale));
        const targetHeight = Math.max(1, Math.round(info.size.height * scale));
        source.createPixelMap({ desiredSize: { width: targetWidth, height: targetHeight } }).then((pixelMap: image.PixelMap): void => {
          const packer = image.createImagePacker();
          packer.packing(pixelMap, { format: 'image/jpeg', quality: 82 }).then((buffer: ArrayBuffer): void => {
            const file = fileIo.openSync(target, fileIo.OpenMode.CREATE | fileIo.OpenMode.READ_WRITE);
            try { fileIo.writeSync(file.fd, buffer); } finally {
              fileIo.closeSync(file); pixelMap.release(); source.release();
            }
            const legacyTarget = this.legacyThumbnailPath(originalDir, original.substring(originalDir.length + 1));
            if (fileIo.accessSync(legacyTarget)) fileIo.unlinkSync(legacyTarget);
            callback(JSON.stringify({ 'status': 'success', 'path': `file://${target}` }));
          }).catch((): void => {
            pixelMap.release(); source.release();
            callback(JSON.stringify({ 'status': 'failure', 'message': '缩略图生成失败。' }));
          });
        }).catch((): void => {
          source.release();
          callback(JSON.stringify({ 'status': 'failure', 'message': '原图无法解码。' }));
        });
      }).catch((): void => {
        source.release();
        callback(JSON.stringify({ 'status': 'failure', 'message': '原图信息无法读取。' }));
      });
    } catch (_) { callback(JSON.stringify({ 'status': 'failure', 'message': '缩略图生成失败。' })); }
  }

  private prepareAvatar(params: KRAny, callback: KuiklyRenderCallback): void {
    const context = this.controller?.getUIAbilityContext();
    if (context === null || context === undefined) return callback(JSON.stringify({ 'status': 'unsupported' }));
    let sourceUri = '';
    try { sourceUri = (JSON.parse(params as string) as MediaThumbnailRequest).path; } catch (_) {}
    const originalDir = `${context.filesDir}/images/original`;
    if (!this.isManagedPath(sourceUri, `file://${originalDir}/`)) {
      callback(JSON.stringify({ 'status': 'failure', 'message': '头像源文件无效。' }));
      return;
    }
    const sourcePath = sourceUri.substring('file://'.length);
    const avatarDir = `${context.filesDir}/images/avatar`;
    const target = `${avatarDir}/avatar_${Date.now()}.jpg`;
    try {
      if (!fileIo.accessSync(avatarDir)) fileIo.mkdirSync(avatarDir, true);
      const source = image.createImageSource(sourcePath);
      source.getImageInfo().then((info: image.ImageInfo): void => {
        const edge = Math.min(info.size.width, info.size.height);
        source.createPixelMap({
          desiredRegion: {
            x: Math.floor((info.size.width - edge) / 2),
            y: Math.floor((info.size.height - edge) / 2),
            size: { width: edge, height: edge }
          },
          desiredSize: { width: 256, height: 256 }
        }).then((pixelMap: image.PixelMap): void => {
          const packer = image.createImagePacker();
          packer.packing(pixelMap, { format: 'image/jpeg', quality: 88 }).then((buffer: ArrayBuffer): void => {
            const file = fileIo.openSync(target, fileIo.OpenMode.CREATE | fileIo.OpenMode.READ_WRITE);
            try { fileIo.writeSync(file.fd, buffer); } finally {
              fileIo.closeSync(file); pixelMap.release(); source.release();
            }
            callback(JSON.stringify({ 'status': 'success', 'path': `file://${target}` }));
          }).catch((): void => {
            pixelMap.release(); source.release();
            callback(JSON.stringify({ 'status': 'failure', 'message': '头像编码失败。' }));
          });
        }).catch((): void => {
          source.release(); callback(JSON.stringify({ 'status': 'failure', 'message': '头像裁剪失败。' }));
        });
      }).catch((): void => {
        source.release(); callback(JSON.stringify({ 'status': 'failure', 'message': '头像原图无法读取。' }));
      });
    } catch (_) {
      callback(JSON.stringify({ 'status': 'failure', 'message': '头像处理失败。' }));
    }
  }

  private deleteAvatar(params: KRAny, callback: KuiklyRenderCallback): void {
    const context = this.controller?.getUIAbilityContext();
    if (context === null || context === undefined) return callback(JSON.stringify({ 'status': 'unsupported' }));
    let avatarUri = '';
    try { avatarUri = (JSON.parse(params as string) as MediaThumbnailRequest).path; } catch (_) {}
    const avatarDir = `${context.filesDir}/images/avatar`;
    if (!this.isManagedPath(avatarUri, `file://${avatarDir}/`)) {
      callback(JSON.stringify({ 'status': 'failure', 'message': '头像路径无效。' }));
      return;
    }
    try {
      const path = avatarUri.substring('file://'.length);
      if (fileIo.accessSync(path)) fileIo.unlinkSync(path);
      callback(JSON.stringify({ 'status': 'success' }));
    } catch (_) {
      callback(JSON.stringify({ 'status': 'failure', 'message': '头像文件无法删除。' }));
    }
  }

  private mediaStatistics(callback: KuiklyRenderCallback): void {
    const context = this.controller?.getUIAbilityContext();
    if (context === null || context === undefined) return callback(JSON.stringify({ 'status': 'unsupported' }));
    const originals = this.files(`${context.filesDir}/images/original`);
    const thumbnails = this.files(`${context.filesDir}/images/thumbnail`);
    callback(JSON.stringify({ 'status': 'success', 'originalBytes': this.bytes(originals).toString(), 'originalCount': originals.length.toString(), 'thumbnailBytes': this.bytes(thumbnails).toString(), 'thumbnailCount': thumbnails.length.toString() }));
  }

  private clearThumbnails(callback: KuiklyRenderCallback): void {
    const context = this.controller?.getUIAbilityContext();
    if (context === null || context === undefined) return callback(JSON.stringify({ 'status': 'unsupported' }));
    callback(this.deleteFiles(this.files(`${context.filesDir}/images/thumbnail`)));
  }

  private cleanupUnreferenced(params: KRAny, callback: KuiklyRenderCallback): void {
    const context = this.controller?.getUIAbilityContext();
    if (context === null || context === undefined) return callback(JSON.stringify({ 'status': 'unsupported' }));
    try {
      const request = JSON.parse(params as string) as MediaCleanupRequest;
      const referenced = new Set(request.paths.map((path: string): string => path.substring('file://'.length)));
      const originalDir = `${context.filesDir}/images/original`;
      const cutoff = Date.now() - Math.max(0, Number(request.gracePeriodMillis));
      const candidates: string[] = [];
      this.files(originalDir).forEach((path: string): void => {
        const stat = fileIo.statSync(path);
        const modifiedMillis = stat.mtime < 100000000000 ? stat.mtime * 1000 : stat.mtime;
        if (!referenced.has(path) && modifiedMillis <= cutoff) {
          const fileName = path.substring(originalDir.length + 1);
          candidates.push(
            path,
            this.thumbnailPath(originalDir, fileName),
            this.legacyThumbnailPath(originalDir, fileName)
          );
        }
      });
      callback(this.deleteFiles(candidates));
    } catch (_) { callback(JSON.stringify({ 'status': 'failure', 'message': '无引用媒体清理失败。' })); }
  }

  private thumbnailPath(originalDir: string, fileName: string): string {
    return `${originalDir.substring(0, originalDir.length - '/original'.length)}/thumbnail/${fileName}.v2.jpg`;
  }

  private legacyThumbnailPath(originalDir: string, fileName: string): string {
    return `${originalDir.substring(0, originalDir.length - '/original'.length)}/thumbnail/${fileName}.jpg`;
  }

  private files(root: string): string[] {
    if (!fileIo.accessSync(root)) return [];
    return fileIo.listFileSync(root).map((name: string): string => `${root}/${name}`).filter((path: string): boolean => fileIo.statSync(path).isFile());
  }

  private bytes(paths: string[]): number { return paths.reduce((sum: number, path: string): number => sum + fileIo.statSync(path).size, 0); }

  private deleteFiles(paths: string[]): string {
    let deletedBytes = 0; let deletedCount = 0;
    paths.forEach((path: string): void => { if (fileIo.accessSync(path)) { const size = fileIo.statSync(path).size; fileIo.unlinkSync(path); deletedBytes += size; deletedCount++; } });
    return JSON.stringify({ 'status': 'success', 'deletedBytes': deletedBytes.toString(), 'deletedCount': deletedCount.toString() });
  }

  private isManagedPath(path: string, prefix: string): boolean {
    if (!path.startsWith(prefix)) {
      return false;
    }
    const fileName = path.substring(prefix.length);
    return fileName.length > 0 && !fileName.includes('/') &&
      !fileName.includes('\\') && fileName !== '.' && fileName !== '..';
  }
}

interface MediaDeleteRequest {
  paths: string[];
}
interface MediaThumbnailRequest { path: string; }
interface MediaCleanupRequest { paths: string[]; gracePeriodMillis: string; }

export function managedImageExtension(sourceUri: string): string {
  const pathWithoutQuery = sourceUri.split('?')[0].split('#')[0];
  const dotIndex = pathWithoutQuery.lastIndexOf('.');
  if (dotIndex < 0 || dotIndex === pathWithoutQuery.length - 1) {
    return 'image';
  }
  const candidate = pathWithoutQuery.substring(dotIndex + 1).toLowerCase();
  const supported = ['jpg', 'jpeg', 'png', 'webp', 'heic', 'heif', 'gif', 'bmp', 'dng'];
  return supported.includes(candidate) ? candidate : 'image';
}
