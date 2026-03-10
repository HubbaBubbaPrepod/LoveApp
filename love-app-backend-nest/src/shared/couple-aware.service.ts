import { CoupleService } from './couple.service';

/**
 * Base class for services that frequently need couple-scoped operations.
 * Eliminates repeated getCoupleKey + broadcastChange boilerplate.
 *
 * Usage:
 *   export class NotesService extends CoupleAwareService {
 *     constructor(coupleService: CoupleService, ...) {
 *       super(coupleService);
 *     }
 *     async create(userId, dto) {
 *       // ...save entity...
 *       await this.broadcast(userId, 'note', 'create', saved);
 *     }
 *   }
 */
export abstract class CoupleAwareService {
  constructor(protected readonly coupleService: CoupleService) {}

  /** Get the couple key for a user (or solo_<id> if unpaired). */
  protected coupleKey(userId: number): Promise<string> {
    return this.coupleService.getCoupleKey(userId);
  }

  /** Get the partner's user ID (or null). */
  protected partnerId(userId: number): Promise<number | null> {
    return this.coupleService.getPartnerId(userId);
  }

  /** Broadcast a data-change event to the couple room. */
  protected async broadcast(
    userId: number,
    entityType: string,
    action: string,
    data?: unknown,
  ): Promise<void> {
    const key = await this.coupleKey(userId);
    await this.coupleService.broadcastChange(key, userId, entityType, action, data);
  }
}
