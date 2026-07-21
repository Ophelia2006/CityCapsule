import { IHMAnimator } from '@hadss/hmrouter';
import { NATIVE_FILE_IMPORT_PAGE, ROUTE_FALLBACK_PAGE } from './HarmonyRoutes';

/** Central transition specification; individual routes may override the global effect. */
export class HarmonyRouteTransitions {
  static standard(): IHMAnimator.Effect {
    return new IHMAnimator.Effect({
      direction: IHMAnimator.Direction.RIGHT_TO_LEFT,
      opacity: { opacity: 0.96 },
      duration: 260,
      curve: 'ease-out'
    });
  }

  static dialog(): IHMAnimator.Effect {
    return new IHMAnimator.Effect({
      direction: IHMAnimator.Direction.BOTTOM_TO_TOP,
      opacity: { opacity: 0.92 },
      scale: { x: 0.98, y: 0.98 },
      duration: 220,
      curve: 'ease-out'
    });
  }

  static pageOverride(pageUrl: string): IHMAnimator | null {
    if (pageUrl === NATIVE_FILE_IMPORT_PAGE) {
      return new IHMAnimator.Effect({
        direction: IHMAnimator.Direction.BOTTOM_TO_TOP,
        duration: 240,
        curve: 'ease-out'
      }).toAnimator();
    }
    if (pageUrl === ROUTE_FALLBACK_PAGE) {
      return new IHMAnimator.Effect({
        opacity: { opacity: 0.0 },
        scale: { x: 0.96, y: 0.96 },
        duration: 180,
        curve: 'ease-out'
      }).toAnimator();
    }
    return null;
  }
}
