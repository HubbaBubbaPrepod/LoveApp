import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('location_updates')
export class LocationUpdate {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column({ type: 'bigint', nullable: true })
  user_id: number;

  @Column({ type: 'varchar', nullable: true })
  couple_key: string;

  @Column({ type: 'numeric', nullable: true })
  latitude: number;

  @Column({ type: 'numeric', nullable: true })
  longitude: number;

  @Column({ type: 'numeric', nullable: true })
  accuracy: number;

  @Column({ type: 'numeric', nullable: true })
  altitude: number;

  @Column({ type: 'numeric', nullable: true })
  speed: number;

  @Column({ type: 'numeric', nullable: true })
  bearing: number;

  @Column({ type: 'varchar', nullable: true })
  provider: string;

  @Column({ type: 'text', nullable: true })
  address: string;

  @Column({ type: 'boolean', nullable: true })
  is_moving: boolean;

  @Column({ type: 'int', nullable: true })
  battery_level: number;

  @CreateDateColumn()
  created_at: Date;
}
