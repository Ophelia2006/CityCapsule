export enum HarmonyBackToDecision {
  COMPLETE = 'complete',
  REPLACE_TARGET = 'replaceTarget'
}

/**
 * A backTo target can legitimately be absent after replace navigation. In that case the target
 * is restored with replace; other stack results mean that backTo has already completed.
 */
export class HarmonyBackToPolicy {
  static decide(closedCount: number): HarmonyBackToDecision {
    return closedCount < 0
      ? HarmonyBackToDecision.REPLACE_TARGET
      : HarmonyBackToDecision.COMPLETE;
  }
}
