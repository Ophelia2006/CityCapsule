import { fileIo } from '@kit.CoreFileKit';
import { KRAny, KuiklyRenderBaseModule, KuiklyRenderCallback } from '@kuikly-open/render';

export class KRTrackModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME: string = 'CCTrackModule';
  syncMode(): boolean { return false; }
  call(method: string, params: KRAny, callback: KuiklyRenderCallback): KRAny {
    if (method === 'readChunks') { this.readChunks(params, callback); return null; }
    if (method !== 'writeChunk') { callback(JSON.stringify({ status: 'unsupported' })); return null; }
    try {
      const body = JSON.parse(params as string) as TrackWriteRequest;
      if (!Array.isArray(body.points) || body.points.length < 1 || body.points.length > 50 || body.chunkIndex < 0) throw new Error('invalid request');
      const context = this.controller?.getUIAbilityContext(); if (context === null || context === undefined) throw new Error('no context');
      const root = `${context.filesDir}/tracks/${body.sessionStartedAt}`; if (!fileIo.accessSync(root)) fileIo.mkdirSync(root, true);
      const target = `${root}/chunk_${body.chunkIndex}.json`; const file = fileIo.openSync(target, fileIo.OpenMode.CREATE | fileIo.OpenMode.READ_WRITE | fileIo.OpenMode.TRUNC);
      try { fileIo.writeSync(file.fd, JSON.stringify(body.points)); } finally { fileIo.closeSync(file); }
      callback(JSON.stringify({ status: 'success', path: `file://${target}` }));
    } catch (_) { callback(JSON.stringify({ status: 'failure', message: '轨迹分片写入失败。' })); }
    return null;
  }
  private readChunks(params: KRAny, callback: KuiklyRenderCallback): void {
    try {
      const body = JSON.parse(params as string) as TrackReadRequest;
      const context = this.controller?.getUIAbilityContext();
      if (!context) throw new Error('no context');
      const prefix = `file://${context.filesDir}/tracks/`;
      const points: Array<Record<string, string | number>> = [];
      body.paths.forEach((uri: string): void => {
        if (!uri.startsWith(prefix)) throw new Error('invalid path');
        const content = fileIo.readTextSync(uri.substring('file://'.length));
        (JSON.parse(content) as Array<Record<string, string | number>>)
          .forEach((point: Record<string, string | number>): void => {
            points.push(point);
          });
      });
      callback(JSON.stringify({ status: 'success', points: points }));
    } catch (_) {
      callback(JSON.stringify({ status: 'failure', message: '轨迹读取失败。' }));
    }
  }
}
interface TrackWriteRequest { sessionStartedAt: string; chunkIndex: number; points: Array<Record<string, string | number>>; }
interface TrackReadRequest { paths: string[]; }
