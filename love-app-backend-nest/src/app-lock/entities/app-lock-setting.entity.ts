import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  UpdateDateColumn,
} from 'typeorm';

@Entity('app_lock_settings')
export class AppLockSetting {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'bigint', unique: true })
  user_id: number;

  @Column({ type: 'varchar' })
  pin_hash: string;

  @Column({ type: 'boolean', default: false })
  is_biometric: boolean;

  @UpdateDateColumn()
  updated_at: Date;
}
