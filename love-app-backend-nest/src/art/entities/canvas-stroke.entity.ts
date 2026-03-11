import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
} from 'typeorm';

@Entity('canvas_strokes')
export class CanvasStroke {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column({ type: 'bigint', nullable: true })
  canvas_id: number;

  @Column({ type: 'text', nullable: true, name: 'strokes_json' })
  strokes_data: string;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  updated_at: Date;
}
