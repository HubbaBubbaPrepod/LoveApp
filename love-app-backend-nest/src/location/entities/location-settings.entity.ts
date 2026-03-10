import { Entity, PrimaryGeneratedColumn, Column, UpdateDateColumn } from 'typeorm';

@Entity('location_settings')
export class LocationSettings {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'bigint', unique: true })
  user_id: number;

  @Column({ type: 'boolean', default: true })
  sharing_enabled: boolean;

  @Column({ type: 'int', default: 300 })
  update_interval: number;

  @Column({ type: 'boolean', default: true })
  show_address: boolean;

  @Column({ type: 'boolean', default: true })
  show_speed: boolean;

  @Column({ type: 'boolean', default: true })
  show_battery: boolean;

  @UpdateDateColumn()
  updated_at: Date;
}
