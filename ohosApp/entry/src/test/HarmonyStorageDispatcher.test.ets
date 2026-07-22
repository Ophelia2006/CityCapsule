import { describe, expect, it } from '@ohos/hypium';
import {
  HarmonyStorageDispatcher
} from '../main/ets/storage/HarmonyStorageDispatcher';
import {
  HarmonyStorageProvider,
  HarmonyStringStore
} from '../main/ets/storage/HarmonyMmkvStorage';
import {
  HarmonyStorageProtocol,
  HarmonyStorageRequest
} from '../main/ets/storage/HarmonyStorageProtocol';

export default function harmonyStorageDispatcherTest(): void {
  describe('HarmonyStorageDispatcher', () => {
    it('putThenGetPersistsCanonicalString', 0, () => {
      const dispatcher = new HarmonyStorageDispatcher(new FakeHarmonyStorageProvider());

      const put = dispatcher.dispatch(
        HarmonyStorageProtocol.METHOD_PUT,
        JSON.stringify(request('dark'))
      );
      const get = dispatcher.dispatch(
        HarmonyStorageProtocol.METHOD_GET,
        JSON.stringify(request())
      );

      expect(put.code).assertEqual(HarmonyStorageProtocol.CODE_SUCCESS);
      expect(get.code).assertEqual(HarmonyStorageProtocol.CODE_SUCCESS);
      expect(get.exists).assertTrue();
      expect(get.value).assertEqual('dark');
    });

    it('typeChangeIsRejected', 0, () => {
      const dispatcher = new HarmonyStorageDispatcher(new FakeHarmonyStorageProvider());
      dispatcher.dispatch(
        HarmonyStorageProtocol.METHOD_PUT,
        JSON.stringify(request('dark'))
      );
      const booleanRequest = request();
      booleanRequest.type = HarmonyStorageProtocol.TYPE_BOOLEAN;

      const response = dispatcher.dispatch(
        HarmonyStorageProtocol.METHOD_GET,
        JSON.stringify(booleanRequest)
      );

      expect(response.code).assertEqual(HarmonyStorageProtocol.ERROR_TYPE_MISMATCH);
    });

    it('unavailableProviderReturnsNotInitialized', 0, () => {
      const provider = new FakeHarmonyStorageProvider();
      provider.ready = false;
      const dispatcher = new HarmonyStorageDispatcher(provider);

      const response = dispatcher.dispatch(
        HarmonyStorageProtocol.METHOD_GET,
        JSON.stringify(request())
      );

      expect(response.code).assertEqual(HarmonyStorageProtocol.ERROR_NOT_INITIALIZED);
    });

    it('malformedJsonReturnsInvalidRequest', 0, () => {
      const dispatcher = new HarmonyStorageDispatcher(new FakeHarmonyStorageProvider());

      const response = dispatcher.dispatch(HarmonyStorageProtocol.METHOD_GET, '{not-json');

      expect(response.code).assertEqual(HarmonyStorageProtocol.ERROR_INVALID_REQUEST);
    });
  });
}

function request(value?: string): HarmonyStorageRequest {
  return {
    protocolVersion: HarmonyStorageProtocol.VERSION,
    store: HarmonyStorageProtocol.STORE_PREFERENCES,
    key: 'settings.theme_mode',
    type: HarmonyStorageProtocol.TYPE_STRING,
    value: value
  };
}

class FakeHarmonyStorageProvider implements HarmonyStorageProvider {
  ready: boolean = true;
  private readonly stores: Map<string, HarmonyStringStore> = new Map<string, HarmonyStringStore>();

  constructor() {
    this.stores.set(HarmonyStorageProtocol.STORE_META, new FakeHarmonyStringStore());
    this.stores.set(HarmonyStorageProtocol.STORE_PREFERENCES, new FakeHarmonyStringStore());
    this.stores.set(HarmonyStorageProtocol.STORE_CACHE, new FakeHarmonyStringStore());
  }

  isReady(): boolean {
    return this.ready;
  }

  store(storeId: string): HarmonyStringStore | undefined {
    return this.stores.get(storeId);
  }
}

class FakeHarmonyStringStore implements HarmonyStringStore {
  private readonly values: Map<string, string> = new Map<string, string>();

  contains(key: string): boolean {
    return this.values.has(key);
  }

  read(key: string): string | undefined {
    return this.values.get(key);
  }

  write(key: string, value: string): boolean {
    this.values.set(key, value);
    return true;
  }

  remove(key: string): void {
    this.values.delete(key);
  }
}
