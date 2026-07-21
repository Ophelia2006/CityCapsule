import { describe, expect, it } from '@ohos/hypium';
import { KRRecord } from '@kuikly-open/render';
import {
  HarmonyRouteAction,
  HarmonyRouteRequestDecoder,
  HarmonyRouteTargetType
} from '../main/ets/navigation/HarmonyRouteRequest';
import { HarmonyRouteProtocol } from '../main/ets/navigation/HarmonyRoutes';

export default function harmonyRouteRequestDecoderTest(): void {
  describe('HarmonyRouteRequestDecoder', () => {
    it('decodesSharedWireContractAndStripsProtocolFields', 0, () => {
      const pageData: KRRecord = {};
      pageData[HarmonyRouteProtocol.PARAM_ACTION] = 'replace';
      pageData[HarmonyRouteProtocol.PARAM_ROUTE_KEY] = 'settings';
      pageData[HarmonyRouteProtocol.PARAM_TARGET_TYPE] = 'kuikly';
      pageData['source'] = 'home';

      const request = HarmonyRouteRequestDecoder.decode('settings', pageData);

      expect(request.action).assertEqual(HarmonyRouteAction.REPLACE);
      expect(request.targetType).assertEqual(HarmonyRouteTargetType.KUIKLY);
      expect(request.routeKey).assertEqual('settings');
      expect(request.target).assertEqual('settings');
      expect(request.pageData['source']).assertEqual('home');
      expect(request.pageData[HarmonyRouteProtocol.PARAM_ACTION]).assertUndefined();
    });

    it('infersNativeTargetForLegacyPayload', 0, () => {
      const request = HarmonyRouteRequestDecoder.decode('/native/permission', {});

      expect(request.action).assertEqual(HarmonyRouteAction.PUSH);
      expect(request.targetType).assertEqual(HarmonyRouteTargetType.NATIVE);
      expect(request.routeKey).assertEqual('/native/permission');
    });
  });
}
