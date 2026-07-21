import hilog from '@ohos.hilog';
import { IHMLog } from '@hadss/hmrouter';

export class HarmonyRouteLogEntry {
  readonly event: string;
  readonly action: string;
  readonly routeKey: string;
  readonly target: string;
  readonly outcome: string;
  readonly reason: string;

  constructor(
    event: string,
    action: string = '',
    routeKey: string = '',
    target: string = '',
    outcome: string = '',
    reason: string = ''
  ) {
    this.event = event;
    this.action = action;
    this.routeKey = routeKey;
    this.target = target;
    this.outcome = outcome;
    this.reason = reason;
  }

  toMessage(): string {
    return `event=${this.clean(this.event)} action=${this.clean(this.action)} ` +
      `routeKey=${this.clean(this.routeKey)} target=${this.clean(this.target)} ` +
      `outcome=${this.clean(this.outcome)} reason=${this.clean(this.reason)}`;
  }

  private clean(value: string): string {
    return value.replace(/\r/g, ' ').replace(/\n/g, ' ');
  }
}

/** One structured log gateway for both application routing and HMRouter internals. */
export class HarmonyRouteLogger implements IHMLog {
  record(entry: HarmonyRouteLogEntry): void {
    this.write('INFO', entry.toMessage());
  }

  rejected(entry: HarmonyRouteLogEntry): void {
    this.write('WARN', entry.toMessage());
  }

  failed(entry: HarmonyRouteLogEntry): void {
    this.write('ERROR', entry.toMessage());
  }

  debug(format: string, ...args: Array<string | number>): void {
    this.writeHmRouter('DEBUG', format, args);
  }

  info(format: string, ...args: Array<string | number>): void {
    this.writeHmRouter('INFO', format, args);
  }

  warn(format: string, ...args: Array<string | number>): void {
    this.writeHmRouter('WARN', format, args);
  }

  error(format: string, ...args: Array<string | number>): void {
    this.writeHmRouter('ERROR', format, args);
  }

  private writeHmRouter(level: string, format: string, args: Array<string | number>): void {
    // HMRouter arguments may include serialized route params. Keep its diagnostic format and
    // argument count, but never copy argument values into application logs.
    const message: string = `event=hmrouter level=${level} message=${format} argCount=${args.length}`;
    this.write(level, message);
  }

  private write(level: string, message: string): void {
    switch (level) {
      case 'DEBUG':
        hilog.debug(HarmonyRouteLogger.DOMAIN, HarmonyRouteLogger.TAG, '%{public}s', message);
        return;
      case 'WARN':
        hilog.warn(HarmonyRouteLogger.DOMAIN, HarmonyRouteLogger.TAG, '%{public}s', message);
        return;
      case 'ERROR':
        hilog.error(HarmonyRouteLogger.DOMAIN, HarmonyRouteLogger.TAG, '%{public}s', message);
        return;
      default:
        hilog.info(HarmonyRouteLogger.DOMAIN, HarmonyRouteLogger.TAG, '%{public}s', message);
    }
  }

  private static readonly DOMAIN: number = 0x0000;
  private static readonly TAG: string = 'CityCapsuleRoute';
  static readonly shared: HarmonyRouteLogger = new HarmonyRouteLogger();
}
