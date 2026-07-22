import {
  HarmonyStorageProtocol,
  HarmonyStorageRequest,
  HarmonyStorageResponse,
  isSupportedStore,
  isSupportedType
} from './HarmonyStorageProtocol';
import {
  HarmonyMmkvStorage,
  HarmonyStorageProvider,
  HarmonyStringStore
} from './HarmonyMmkvStorage';

export class HarmonyStorageDispatcher {
  private static readonly TYPE_KEY_PREFIX: string = '__cc_type__.';
  private static readonly WIRE_KEY: RegExp = /^[a-z][a-z0-9_]*\.[a-z][a-z0-9_]*$/;

  private readonly provider: HarmonyStorageProvider;

  constructor(provider: HarmonyStorageProvider = HarmonyMmkvStorage.shared) {
    this.provider = provider;
  }

  dispatch(method: string, params: string): HarmonyStorageResponse {
    let request: HarmonyStorageRequest;
    try {
      request = JSON.parse(params) as HarmonyStorageRequest;
    } catch (_) {
      return this.invalidRequest();
    }
    try {
      switch (method) {
        case HarmonyStorageProtocol.METHOD_GET:
          return this.get(request);
        case HarmonyStorageProtocol.METHOD_PUT:
          return this.put(request);
        case HarmonyStorageProtocol.METHOD_REMOVE:
          return this.remove(request);
        case HarmonyStorageProtocol.METHOD_CONTAINS:
          return this.contains(request);
        case HarmonyStorageProtocol.METHOD_GET_MANY:
          return this.getMany(request);
        default:
          return this.failure(
            HarmonyStorageProtocol.ERROR_UNKNOWN_METHOD,
            `Unknown storage method '${method}'.`
          );
      }
    } catch (_) {
      return this.failure(
        HarmonyStorageProtocol.ERROR_NATIVE_FAILURE,
        'Harmony storage operation failed.'
      );
    }
  }

  private get(request: HarmonyStorageRequest): HarmonyStorageResponse {
    if (!this.isValidSingleRequest(request, false)) {
      return this.invalidRequest();
    }
    const store = this.readyStore(request.store);
    if (store === undefined) {
      return this.notInitialized();
    }
    return this.read(store, request);
  }

  private put(request: HarmonyStorageRequest): HarmonyStorageResponse {
    if (!this.isValidSingleRequest(request, true) || request.value === undefined) {
      return this.invalidRequest();
    }
    const store = this.readyStore(request.store);
    if (store === undefined) {
      return this.notInitialized();
    }
    const typeKey = this.typeKey(request.key);
    const typeWritten = store.write(typeKey, request.type);
    const valueWritten = typeWritten && store.write(request.key, request.value);
    if (!valueWritten) {
      if (typeWritten) {
        store.remove(typeKey);
      }
      return this.failure(
        HarmonyStorageProtocol.ERROR_NATIVE_FAILURE,
        `Failed to write '${request.key}'.`
      );
    }
    return this.success(true);
  }

  private remove(request: HarmonyStorageRequest): HarmonyStorageResponse {
    if (!this.isValidSingleRequest(request, false)) {
      return this.invalidRequest();
    }
    const store = this.readyStore(request.store);
    if (store === undefined) {
      return this.notInitialized();
    }
    store.remove(request.key);
    store.remove(this.typeKey(request.key));
    return this.success(false);
  }

  private contains(request: HarmonyStorageRequest): HarmonyStorageResponse {
    if (!this.isValidSingleRequest(request, false)) {
      return this.invalidRequest();
    }
    const store = this.readyStore(request.store);
    if (store === undefined) {
      return this.notInitialized();
    }
    return this.success(store.contains(request.key));
  }

  private getMany(request: HarmonyStorageRequest): HarmonyStorageResponse {
    if (request.protocolVersion !== HarmonyStorageProtocol.VERSION || request.entries === undefined) {
      return this.invalidRequest();
    }
    const responses: HarmonyStorageResponse[] = [];
    request.entries.forEach((entry: HarmonyStorageRequest) => {
      const response = this.get(entry);
      response.key = entry.key;
      responses.push(response);
    });
    const result = this.success(false);
    result.entries = responses;
    return result;
  }

  private read(store: HarmonyStringStore, request: HarmonyStorageRequest): HarmonyStorageResponse {
    if (!store.contains(request.key)) {
      return {
        code: HarmonyStorageProtocol.CODE_MISSING,
        message: '',
        exists: false
      };
    }
    const storedType = store.read(this.typeKey(request.key));
    if (storedType !== undefined && storedType !== request.type) {
      const response = this.failure(
        HarmonyStorageProtocol.ERROR_TYPE_MISMATCH,
        `Stored type for '${request.key}' is '${storedType}'.`
      );
      response.exists = true;
      return response;
    }
    const value = store.read(request.key);
    if (value === undefined) {
      const response = this.failure(
        HarmonyStorageProtocol.ERROR_NATIVE_FAILURE,
        `Stored value for '${request.key}' is unavailable.`
      );
      response.exists = true;
      return response;
    }
    const response = this.success(true);
    response.value = value;
    return response;
  }

  private isValidSingleRequest(request: HarmonyStorageRequest, requireValue: boolean): boolean {
    if (request.protocolVersion !== HarmonyStorageProtocol.VERSION ||
      !isSupportedStore(request.store) ||
      !HarmonyStorageDispatcher.WIRE_KEY.test(request.key) ||
      !isSupportedType(request.type)) {
      return false;
    }
    return !requireValue || request.value !== undefined;
  }

  private readyStore(storeId: string): HarmonyStringStore | undefined {
    if (!this.provider.isReady()) {
      return undefined;
    }
    return this.provider.store(storeId);
  }

  private success(exists: boolean): HarmonyStorageResponse {
    return {
      code: HarmonyStorageProtocol.CODE_SUCCESS,
      message: '',
      exists: exists
    };
  }

  private invalidRequest(): HarmonyStorageResponse {
    return this.failure(
      HarmonyStorageProtocol.ERROR_INVALID_REQUEST,
      `Storage request does not match protocol v${HarmonyStorageProtocol.VERSION}.`
    );
  }

  private notInitialized(): HarmonyStorageResponse {
    return this.failure(
      HarmonyStorageProtocol.ERROR_NOT_INITIALIZED,
      'Harmony MMKV storage is not initialized.'
    );
  }

  private failure(code: number, message: string): HarmonyStorageResponse {
    return {
      code: code,
      message: message,
      exists: false
    };
  }

  private typeKey(key: string): string {
    return HarmonyStorageDispatcher.TYPE_KEY_PREFIX + key;
  }
}
