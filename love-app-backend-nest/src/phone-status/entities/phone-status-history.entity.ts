import { Entity, PrimaryGeneratedColumn, Column } from 'typeorm';

@Entity('phone_status_history')
export class PhoneStatusHistory {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'varchar', nullable: true })
  couple_key: string;

  @Column({ type: 'int', nullable: true })
  battery_level: number;

  @Column({ type: 'boolean', nullable: true })
  is_charging: boolean;

  @Column({ type: 'varchar', nullable: true })
  screen_status: string;

  @Column({ type: 'varchar', nullable: true })
  wifi_name: string;

  @Column({ type: 'boolean', nullable: true })
  is_active: boolean;

  @Column({ type: 'boolean', nullable: true })
  app_in_foreground: boolean;

  @Column({ type: 'varchar', nullable: true })
  network_type: string;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  recorded_at: Date;
}
