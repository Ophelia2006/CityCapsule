class HarmonyRouteStackEntry {
  readonly id: number;
  readonly routeKey: string;

  constructor(id: number, routeKey: string) {
    this.id = id;
    this.routeKey = routeKey;
  }
}

/**
 * Tracks stable business route keys independently of HMRouter page URLs.
 * All Kuikly pages share one HMRouter host URL, so HMRouter cannot implement
 * backTo(routeKey) without this business-level stack.
 */
export class HarmonyRouteStackCoordinator {
  private entries: Array<HarmonyRouteStackEntry> = [];
  private nextId: number = 1;

  register(routeKey: string): number {
    const normalized: string = routeKey.trim();
    if (normalized.length === 0) {
      throw new Error('Harmony route host key must not be blank.');
    }
    const id: number = this.nextId++;
    this.entries.push(new HarmonyRouteStackEntry(id, normalized));
    return id;
  }

  unregister(entryId: number): void {
    const index: number = this.entries.findIndex((entry: HarmonyRouteStackEntry) => entry.id === entryId);
    if (index >= 0) {
      this.entries.splice(index, 1);
    }
  }

  /** Returns pages to close, zero for current target, and -1 when not found. */
  backTo(routeKey: string): number {
    const normalized: string = routeKey.trim();
    if (normalized.length === 0) {
      throw new Error('Harmony backTo routeKey must not be blank.');
    }
    let targetIndex: number = -1;
    for (let index: number = this.entries.length - 1; index >= 0; index--) {
      if (this.entries[index].routeKey === normalized) {
        targetIndex = index;
        break;
      }
    }
    if (targetIndex < 0) {
      return -1;
    }
    const closedCount: number = this.entries.length - targetIndex - 1;
    if (closedCount > 0) {
      this.entries.splice(targetIndex + 1, closedCount);
    }
    return closedCount;
  }

  snapshotRouteKeys(): Array<string> {
    return this.entries.map((entry: HarmonyRouteStackEntry) => entry.routeKey);
  }

  clear(): void {
    this.entries = [];
    this.nextId = 1;
  }

  static readonly shared: HarmonyRouteStackCoordinator = new HarmonyRouteStackCoordinator();
}
