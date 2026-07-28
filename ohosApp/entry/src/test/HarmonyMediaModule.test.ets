import { describe, expect, it } from '@ohos/hypium';
import { managedImageExtension } from '../main/ets/kuikly/modules/KRMediaModule';

export default function harmonyMediaModuleTest() {
  describe('HarmonyMediaModule', () => {
    it('preservesSupportedImageExtensionsWithoutQueryData', 0, () => {
      expect(managedImageExtension('file://media/Photo/1/photo.JPEG?token=private'))
        .assertEqual('jpeg');
      expect(managedImageExtension('file://media/Photo/2/photo.heic#selection'))
        .assertEqual('heic');
    });

    it('usesNeutralExtensionForUnknownOrMissingSuffixes', 0, () => {
      expect(managedImageExtension('file://media/Photo/3/photo.custom'))
        .assertEqual('image');
      expect(managedImageExtension('file://media/Photo/4/asset'))
        .assertEqual('image');
    });
  });
}
