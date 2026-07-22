import { common } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { MMKV } from '@tencent/mmkv';
import { HarmonyStorageProtocol } from './HarmonyStorageProtocol';

export interface HarmonyStringStore {
  contains(key: string): boolean;

  read(key: string): string | undefined;

  write(key: string, value: string): boolean;

  remove(key: string): void;
}

export interface HarmonyStorageProvider {
  isReady(): boolean;

  store(storeId: string): HarmonyStringStore | undefined;
}

export interface HarmonyStorageInitialization {
  success: boolean;
  rootPath?: string;
  message?: string;
}

export class HarmonyMmkvStorage implements HarmonyStorageProvider {
  static readonly shared: HarmonyMmkvStorage = new HarmonyMmkvStorage();

  private readonly stores: Map<string, HarmonyStringStore> = new Map<string, HarmonyStringStore>();
  private ready: boolean = false;
  private errorMessage: string | undefined = undefined;

  private constructor() {
  }

  initialize(context: common.ApplicationContext): HarmonyStorageInitialization {
    if (this.ready) {
      return {
        success: true,
        rootPath: MMKV.rootDir
      };
    }
    try {
      const rootPath = MMKV.initialize(context);
      const openedStores: Map<string, HarmonyStringStore> = new Map<string, HarmonyStringStore>();
      openedStores.set(
        HarmonyStorageProtocol.STORE_META,
        new HarmonyMmkvStringStore(MMKV.mmkvWithID(HarmonyStorageProtocol.STORE_META, MMKV.SINGLE_PROCESS_MODE))
      );
      openedStores.set(
        HarmonyStorageProtocol.STORE_PREFERENCES,
        new HarmonyMmkvStringStore(
          MMKV.mmkvWithID(HarmonyStorageProtocol.STORE_PREFERENCES, MMKV.SINGLE_PROCESS_MODE)
        )
      );
      openedStores.set(
        HarmonyStorageProtocol.STORE_CACHE,
        new HarmonyMmkvStringStore(MMKV.mmkvWithID(HarmonyStorageProtocol.STORE_CACHE, MMKV.SINGLE_PROCESS_MODE))
      );
      this.stores.clear();
      openedStores.forEach((store: HarmonyStringStore, storeId: string) => {
        this.stores.set(storeId, store);
      });
      this.errorMessage = undefined;
      this.ready = true;
      hilog.info(0x0000, 'CityCapsuleStorage', 'MMKV initialized at %{public}s', rootPath);
      return {
        success: true,
        rootPath: rootPath
      };
    } catch (error) {
      this.stores.clear();
      this.ready = false;
      this.errorMessage = error instanceof Error ? error.message : 'Unknown MMKV initialization error.';
      hilog.error(
        0x0000,
        'CityCapsuleStorage',
        'MMKV initialization failed: %{public}s',
        this.errorMessage
      );
      return {
        success: false,
        message: this.errorMessage
      };
    }
  }

  isReady(): boolean {
    return this.ready;
  }

  store(storeId: string): HarmonyStringStore | undefined {
    return this.stores.get(storeId);
  }

  lastError(): string | undefined {
    return this.errorMessage;
  }
}

class HarmonyMmkvStringStore implements HarmonyStringStore {
  private readonly mmkv: MMKV;

  constructor(mmkv: MMKV) {
    this.mmkv = mmkv;
  }

  contains(key: string): boolean {
    return this.mmkv.containsKey(key);
  }

  read(key: string): string | undefined {
    return this.mmkv.decodeString(key);
  }

  write(key: string, value: string): boolean {
    return this.mmkv.encodeString(key, value);
  }

  remove(key: string): void {
    this.mmkv.removeValueForKey(key);
  }
}

