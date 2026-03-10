import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
} from 'typeorm';

@Entity('user_sticker_packs')
export class UserStickerPack {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'int' })
  pack_id: number;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  acquired_at: Date;
}
