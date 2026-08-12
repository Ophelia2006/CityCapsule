import { zlib } from '@kit.BasicServicesKit';
import { util } from '@kit.ArkTS';
import { fileIo, picker } from '@kit.CoreFileKit';
import {
  KRAny,
  KuiklyRenderBaseModule,
  KuiklyRenderCallback
} from '@kuikly-open/render';

export class KRDataArchiveModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME: string = 'CCDataArchiveModule';
  private fileOperationInFlight: boolean = false;

  syncMode(): boolean {
    return false;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback): KRAny {
    switch (method) {
      case 'storageUsage':
        callback(this.usage());
        break;
      case 'clearTemporaryFiles':
        callback(this.clearTemporaryFiles());
        break;
      case 'exportArchive':
        this.exportArchive(params as string, callback);
        break;
      case 'selectImport':
        this.selectImport(callback);
        break;
      case 'createRecovery':
        this.createRecovery(params as string, callback);
        break;
      case 'commitImportedMedia':
        callback(this.commitImportedMedia(params as string));
        break;
      case 'discardImport':
        callback(this.discardImport(params as string));
        break;
      default:
        callback(response('unsupported', '当前数据文件方法不受支持。'));
    }
    return null;
  }

  private async exportArchive(params: string, callback: KuiklyRenderCallback): Promise<void> {
    if (!this.beginFileOperation(callback)) {
      return;
    }
    let tempZip: string = '';
    try {
      tempZip = await this.buildArchive(params, false);
      const context = this.requireContext();
      const documentPicker = new picker.DocumentViewPicker(context);
      const options = new picker.DocumentSaveOptions();
      options.newFileNames = [`citycapsule-backup-${Date.now()}.zip`];
      const targets = await documentPicker.save(options);
      if (targets.length === 0) {
        callback(response('cancelled'));
      } else {
        this.copyToUri(tempZip, targets[0]);
        callback(response('success', '', { 'path': targets[0] }));
      }
    } catch (error) {
      if (isPickerCancelled(error)) {
        callback(response('cancelled'));
      } else {
        callback(response('failure', '无法创建或保存备份文件。'));
      }
    } finally {
      this.fileOperationInFlight = false;
      safeDelete(tempZip);
    }
  }

  private async selectImport(callback: KuiklyRenderCallback): Promise<void> {
    if (!this.beginFileOperation(callback)) {
      return;
    }
    try {
      const context = this.requireContext();
      const documentPicker = new picker.DocumentViewPicker(context);
      const options = new picker.DocumentSelectOptions();
      options.fileSuffixFilters = ['.zip'];
      options.maxSelectNumber = 1;
      const selected = await documentPicker.select(options);
      if (selected.length === 0) {
        callback(response('cancelled'));
        return;
      }
      const sessionId = `${Date.now()}-${Math.floor(Math.random() * 1000000)}`;
      const session = `${this.stagingRoot()}/${sessionId}`;
      ensureDirectory(session);
      const archive = `${session}/selected.zip`;
      this.copyFromUri(selected[0], archive);
      await zlib.decompressFile(archive, session, {});
      const payloadPath = `${session}/data/backup.json`;
      if (!fileIo.accessSync(payloadPath)) {
        throw new Error('Backup payload is missing.');
      }
      const payload = readUtf8(payloadPath, 8 * 1024 * 1024);
      JSON.parse(payload);
      callback(response('success', '', {
        'sessionId': sessionId,
        'payload': payload,
        'fileName': 'CityCapsule 备份'
      }));
    } catch (error) {
      if (isPickerCancelled(error)) {
        callback(response('cancelled'));
      } else {
        callback(response('failure', '无法读取此备份；文件可能损坏或格式不受支持。'));
      }
    } finally {
      this.fileOperationInFlight = false;
    }
  }

  private async createRecovery(params: string, callback: KuiklyRenderCallback): Promise<void> {
    try {
      const path = await this.buildArchive(params, true);
      callback(response('success', '', { 'path': path }));
    } catch (_) {
      callback(response('failure', '无法创建导入前备份，导入已停止。'));
    }
  }

  private commitImportedMedia(params: string): string {
    const created: string[] = [];
    try {
      const request = JSON.parse(params) as SessionRequest;
      requireSessionId(request.sessionId);
      const session = `${this.stagingRoot()}/${request.sessionId}`;
      const indexPath = `${session}/media/index.json`;
      const index = fileIo.accessSync(indexPath) ?
        JSON.parse(readUtf8(indexPath, 1024 * 1024)) as Record<string, string> :
        JSON.parse('{}') as Record<string, string>;
      const mediaRoot = this.mediaRoot();
      ensureDirectory(mediaRoot);
      const mapping = JSON.parse('{}') as Record<string, string>;
      Object.keys(index).forEach((oldPath: string, position: number): void => {
        const entry = index[oldPath];
        if (!entry.startsWith('media/images/') || entry.includes('..')) {
          throw new Error('Unsafe media entry.');
        }
        const source = `${session}/${entry}`;
        if (!fileIo.accessSync(source)) {
          return;
        }
        const extension = safeExtension(source);
        const target = `${mediaRoot}/import_${Date.now()}_${position}.${extension}`;
        fileIo.copyFileSync(source, target);
        const newPath = `file://${target}`;
        mapping[oldPath] = newPath;
        created.push(newPath);
      });
      safeRemoveDirectory(session);
      return response('success', '', { 'pathMapping': mapping, 'createdPaths': created });
    } catch (_) {
      created.forEach((path: string): void => safeDelete(path.substring('file://'.length)));
      return response('failure', '照片恢复失败，尚未写入导入数据。');
    }
  }

  private discardImport(params: string): string {
    try {
      const request = JSON.parse(params) as SessionRequest;
      requireSessionId(request.sessionId);
      safeRemoveDirectory(`${this.stagingRoot()}/${request.sessionId}`);
      return response('success');
    } catch (_) {
      return response('failure', '临时导入文件未能清理。');
    }
  }

  private usage(): string {
    return response('success', '', {
      'mediaBytes': directoryBytes(this.mediaRoot()).toString(),
      'cacheBytes': (directoryBytes(this.stagingRoot()) + directoryBytes(this.exportRoot())).toString(),
      'recoveryBytes': directoryBytes(this.recoveryRoot()).toString(),
      'cacheCount': (directoryFileCount(this.stagingRoot()) + directoryFileCount(this.exportRoot())).toString(),
      'recoveryCount': directoryFileCount(this.recoveryRoot()).toString()
    });
  }

  private clearTemporaryFiles(): string {
    const bytes = directoryBytes(this.stagingRoot()) + directoryBytes(this.exportRoot());
    safeRemoveDirectory(this.stagingRoot());
    safeRemoveDirectory(this.exportRoot());
    return response('success', '', { 'clearedBytes': bytes.toString() });
  }

  private async buildArchive(params: string, recovery: boolean): Promise<string> {
    const request = JSON.parse(params) as ArchiveRequest;
    if (request.payload.length > 8 * 1024 * 1024) {
      throw new Error('Payload is too large.');
    }
    const root = recovery ? this.recoveryRoot() : this.exportRoot();
    ensureDirectory(root);
    if (recovery) {
      safeRemoveDirectory(root);
      ensureDirectory(root);
    }
    const work = `${this.exportRoot()}/work-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
    ensureDirectory(`${work}/data`);
    ensureDirectory(`${work}/media/images`);
    writeUtf8(`${work}/data/backup.json`, request.payload);
    const index: Record<string, string> = {};
    request.mediaPaths.forEach((path: string, position: number): void => {
      const source = this.resolveManagedMedia(path);
      if (source === null) {
        return;
      }
      const entry = `media/images/${position}_${source.substring(source.lastIndexOf('/') + 1)}`;
      fileIo.copyFileSync(source, `${work}/${entry}`);
      index[path] = entry;
    });
    writeUtf8(`${work}/media/index.json`, JSON.stringify(index));
    const target = `${root}/${recovery ? 'before-import' : 'citycapsule-export'}-${Date.now()}.zip`;
    await zlib.compressFile(work, target, {});
    safeRemoveDirectory(work);
    return target;
  }

  private beginFileOperation(callback: KuiklyRenderCallback): boolean {
    if (this.fileOperationInFlight) {
      callback(response('failure', '已有文件操作正在进行。'));
      return false;
    }
    this.fileOperationInFlight = true;
    return true;
  }

  private requireContext() {
    const context = this.controller?.getUIAbilityContext();
    if (context === null || context === undefined) {
      throw new Error('UIAbilityContext is unavailable.');
    }
    return context;
  }

  private filesDir(): string {
    return this.requireContext().filesDir;
  }

  private cacheDir(): string {
    return this.requireContext().cacheDir;
  }

  private mediaRoot(): string {
    return `${this.filesDir()}/images/original`;
  }

  private recoveryRoot(): string {
    return `${this.filesDir()}/backups/recovery`;
  }

  private stagingRoot(): string {
    return `${this.cacheDir()}/data-import`;
  }

  private exportRoot(): string {
    return `${this.cacheDir()}/data-export`;
  }

  private resolveManagedMedia(path: string): string | null {
    const prefix = `file://${this.mediaRoot()}/`;
    if (!path.startsWith(prefix)) {
      return null;
    }
    const name = path.substring(prefix.length);
    if (name.length === 0 || name.includes('/') || name.includes('\\') || name.includes('..')) {
      return null;
    }
    const source = `${this.mediaRoot()}/${name}`;
    return fileIo.accessSync(source) ? source : null;
  }

  private copyToUri(source: string, targetUri: string): void {
    const target = fileIo.openSync(targetUri, fileIo.OpenMode.READ_WRITE | fileIo.OpenMode.CREATE |
      fileIo.OpenMode.TRUNC);
    try {
      fileIo.copyFileSync(source, target.fd);
    } finally {
      fileIo.closeSync(target);
    }
  }

  private copyFromUri(sourceUri: string, target: string): void {
    const source = fileIo.openSync(sourceUri);
    try {
      fileIo.copyFileSync(source.fd, target);
    } finally {
      fileIo.closeSync(source);
    }
  }
}

interface ArchiveRequest {
  payload: string;
  mediaPaths: string[];
}

interface SessionRequest {
  sessionId: string;
}

function response(
  status: string,
  message: string = '',
  values: Record<string, Object> = JSON.parse('{}') as Record<string, Object>
): string {
  const result = JSON.parse('{}') as Record<string, Object>;
  result['status'] = status;
  result['message'] = message;
  Object.keys(values).forEach((key: string): void => {
    result[key] = values[key];
  });
  return JSON.stringify(result);
}

function ensureDirectory(path: string): void {
  if (!fileIo.accessSync(path)) {
    fileIo.mkdirSync(path, true);
  }
}

function writeUtf8(path: string, value: string): void {
  const file = fileIo.openSync(path, fileIo.OpenMode.CREATE | fileIo.OpenMode.READ_WRITE |
    fileIo.OpenMode.TRUNC);
  try {
    fileIo.writeSync(file.fd, value, { encoding: 'utf-8' });
  } finally {
    fileIo.closeSync(file);
  }
}

function readUtf8(path: string, maxBytes: number): string {
  const stat = fileIo.statSync(path);
  if (stat.size > maxBytes) {
    throw new Error('File is too large.');
  }
  const file = fileIo.openSync(path, fileIo.OpenMode.READ_ONLY);
  try {
    const buffer = new ArrayBuffer(stat.size);
    const read = fileIo.readSync(file.fd, buffer);
    return new util.TextDecoder('utf-8').decode(new Uint8Array(buffer, 0, read));
  } finally {
    fileIo.closeSync(file);
  }
}

function directoryBytes(root: string): number {
  if (!fileIo.accessSync(root)) {
    return 0;
  }
  let total = 0;
  const files = fileIo.listFileSync(root, { recursion: true });
  files.forEach((relative: string): void => {
    const path = `${root}/${relative}`;
    try {
      const stat = fileIo.statSync(path);
      if (stat.isFile()) {
        total += stat.size;
      }
    } catch (_) {
    }
  });
  return total;
}

function directoryFileCount(root: string): number {
  if (!fileIo.accessSync(root)) return 0;
  let count = 0;
  fileIo.listFileSync(root, { recursion: true }).forEach((relative: string): void => {
    try { if (fileIo.statSync(`${root}/${relative}`).isFile()) count++; } catch (_) {}
  });
  return count;
}

function safeDelete(path: string): void {
  if (path.length === 0) {
    return;
  }
  try {
    if (fileIo.accessSync(path)) {
      fileIo.unlinkSync(path);
    }
  } catch (_) {
  }
}

function safeRemoveDirectory(path: string): void {
  if (path.length === 0) {
    return;
  }
  try {
    if (fileIo.accessSync(path)) {
      fileIo.rmdirSync(path);
    }
  } catch (_) {
  }
}

function safeExtension(path: string): string {
  const name = path.substring(path.lastIndexOf('/') + 1);
  const dot = name.lastIndexOf('.');
  const candidate = dot >= 0 ? name.substring(dot + 1).toLowerCase() : 'image';
  return /^[a-z0-9]{1,8}$/.test(candidate) ? candidate : 'image';
}

function requireSessionId(value: string): void {
  if (!/^[0-9]+-[0-9]+$/.test(value)) {
    throw new Error('Invalid import session.');
  }
}

function isPickerCancelled(error: Object): boolean {
  const code = (error as Record<string, number>)['code'] ?? -1;
  return code === 13900012 || code === 1600003;
}
