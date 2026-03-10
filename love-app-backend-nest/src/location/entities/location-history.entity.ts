import { Entity, PrimaryGeneratedColumn, Column } from 'typeorm';

@Entity('location_history')
export class LocationHistory {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'varchar', nullable: true })
  couple_key: string;

  @Column({ type: 'numeric' })
  latitude: number;

  @Column({ type: 'numeric' })
  longitude: number;

  @Column({ type: 'numeric', nullable: true })
  accuracy: number;

  @Column({ type: 'numeric', nullable: true })
  speed: number;

  @Column({ type: 'text', nullable: true })
  address: string;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  recorded_at: Date;
}
