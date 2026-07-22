export class HarmonyStorageProtocol {
  static readonly VERSION: number = 1;
  static readonly MODULE_NAME: string = 'CCStorageModule';

  static readonly METHOD_GET: string = 'storageGet';
  static readonly METHOD_PUT: string = 'storagePut';
  static readonly METHOD_REMOVE: string = 'storageRemove';
  static readonly METHOD_CONTAINS: string = 'storageContains';
  static readonly METHOD_GET_MANY: string = 'storageGetMany';

  static readonly STORE_META: string = 'cc_meta';
  static readonly STORE_PREFERENCES: string = 'cc_preferences';
  static readonly STORE_CACHE: string = 'cc_cache';

  static readonly TYPE_STRING: string = 'string';
  static readonly TYPE_BOOLEAN: string = 'boolean';
  static readonly TYPE_LONG: string = 'long';
  static readonly TYPE_DOUBLE: string = 'double';
  static readonly TYPE_JSON_OBJECT: string = 'json_object';

  static readonly CODE_SUCCESS: number = 0;
  static readonly CODE_MISSING: number = 1;
  static readonly ERROR_INVALID_REQUEST: number = 1001;
  static readonly ERROR_NOT_INITIALIZED: number = 1002;
  static readonly ERROR_TYPE_MISMATCH: number = 1003;
  static readonly ERROR_DECODE_FAILED: number = 1004;
  static readonly ERROR_NATIVE_FAILURE: number = 1005;
  static readonly ERROR_UNKNOWN_METHOD: number = 1006;
  static readonly ERROR_UNKNOWN: number = 1999;
}

export interface HarmonyStorageRequest {
  protocolVersion: number;
  store: string;
  key: string;
  type: string;
  value?: string;
  entries?: HarmonyStorageRequest[];
}

export interface HarmonyStorageResponse {
  code: number;
  message: string;
  exists: boolean;
  key?: string;
  value?: string;
  entries?: HarmonyStorageResponse[];
}

export function isSupportedStore(store: string): boolean {
  return store === HarmonyStorageProtocol.STORE_META ||
    store === HarmonyStorageProtocol.STORE_PREFERENCES ||
    store === HarmonyStorageProtocol.STORE_CACHE;
}

export function isSupportedType(type: string): boolean {
  return type === HarmonyStorageProtocol.TYPE_STRING ||
    type === HarmonyStorageProtocol.TYPE_BOOLEAN ||
    type === HarmonyStorageProtocol.TYPE_LONG ||
    type === HarmonyStorageProtocol.TYPE_DOUBLE ||
    type === HarmonyStorageProtocol.TYPE_JSON_OBJECT;
}

