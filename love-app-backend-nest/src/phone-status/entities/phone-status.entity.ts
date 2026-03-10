import { Entity, PrimaryGeneratedColumn, Column, UpdateDateColumn } from 'typeorm';

@Entity('phone_status')
export class PhoneStatus {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'bigint', unique: true })
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

  @Column({ type: 'boolean', default: false })
  is_active: boolean;

  @Column({ type: 'timestamp', nullable: true })
  last_active_at: Date | null;

  @Column({ type: 'boolean', default: false })
  app_in_foreground: boolean;

  @Column({ type: 'varchar', nullable: true })
  network_type: string;

  @UpdateDateColumn()
  updated_at: Date;
}
