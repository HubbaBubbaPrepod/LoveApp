import { Entity, PrimaryGeneratedColumn, Column } from 'typeorm';

@Entity('geofence_events')
export class GeofenceEvent {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column({ type: 'bigint' })
  geofence_id: number;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'varchar', nullable: true })
  couple_key: string;

  @Column({ type: 'varchar' })
  event_type: string;

  @Column({ type: 'numeric', nullable: true })
  latitude: number;

  @Column({ type: 'numeric', nullable: true })
  longitude: number;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  triggered_at: Date;
}
