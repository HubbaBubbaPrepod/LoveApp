import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';

@Entity('geofences')
export class Geofence {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'varchar', nullable: true })
  couple_key: string;

  @Column({ type: 'varchar' })
  name: string;

  @Column({ type: 'numeric' })
  latitude: number;

  @Column({ type: 'numeric' })
  longitude: number;

  @Column({ type: 'int' })
  radius_meters: number;

  @Column({ type: 'varchar', nullable: true })
  category: string;

  @Column({ type: 'text', nullable: true })
  address: string;

  @Column({ type: 'varchar', default: '📍' })
  icon: string;

  @Column({ type: 'varchar', default: '#FF6B9D' })
  color: string;

  @Column({ type: 'boolean', default: true })
  notify_on_enter: boolean;

  @Column({ type: 'boolean', default: true })
  notify_on_exit: boolean;

  @Column({ type: 'boolean', default: true })
  is_active: boolean;

  @CreateDateColumn()
  created_at: Date;

  @UpdateDateColumn()
  updated_at: Date;
}
