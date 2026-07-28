import { BusinessError } from '@kit.BasicServicesKit';
import { fileIo } from '@kit.CoreFileKit';
import { photoAccessHelper } from '@kit.MediaLibraryKit';
import hilog from '@ohos.hilog';
import {
  KRAny,
  KuiklyRenderBaseModule,
  KuiklyRenderCallback
} from '@kuikly-open/render';

export class KRMediaModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME: string = 'CCMediaModule';
  private static readonly METHOD_PICK_IMAGES: string = 'pickImages';
  private static readonly METHOD_DELETE_IMAGES: string = 'deleteImages';
  private static readonly MAX_IMAGES: number = 9;
  private static readonly MAX_DELETE_BATCH: number = 32;
  private static readonly LOG_DOMAIN: number = 0x0000;
  private static readonly LOG_TAG: string = 'CityCapsuleMedia';
  private pickerInFlight: boolean = false;

  syncMode(): boolean {
    return false;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback): KRAny {
    if (method === KRMediaModule.METHOD_PICK_IMAGES) {
      this.pickImages(params, callback);
    } else if (method === KRMediaModule.METHOD_DELETE_IMAGES) {
      this.deleteImages(params, callback);
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

    if (this.pickerInFlight) {
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
        deleted.push(path);
      });
      callback(JSON.stringify({ 'status': 'success', 'message': '', 'paths': deleted }));
    } catch (_) {
      callback(JSON.stringify({ 'status': 'failure', 'message': '照片文件暂时无法清理。', 'paths': deleted }));
    }
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
