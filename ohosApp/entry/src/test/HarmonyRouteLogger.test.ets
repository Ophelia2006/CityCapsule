import { describe, expect, it } from '@ohos/hypium';
import { HarmonyRouteLogEntry } from '../main/ets/navigation/HarmonyRouteLogger';

export default function harmonyRouteLoggerTest() {
  describe('HarmonyRouteLogEntry', () => {
    it('writesStableStructuredFields', 0, () => {
      const message: string = new HarmonyRouteLogEntry(
        'dispatch', 'push', 'settings', 'settings', 'accepted'
      ).toMessage();

      expect(message).assertContain('event=dispatch');
      expect(message).assertContain('routeKey=settings');
      expect(message).assertContain('outcome=accepted');
    });

    it('keepsLogOnOneLine', 0, () => {
      const message: string = new HarmonyRouteLogEntry(
        'failure', 'push', 'home', 'home', 'fallback', 'line one\nline two'
      ).toMessage();

      expect(message.includes('\n')).assertFalse();
      expect(message).assertContain('line one line two');
    });
  });
}
