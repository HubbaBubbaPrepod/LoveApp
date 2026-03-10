import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  UpdateDateColumn,
} from 'typeorm';

@Entity('spark_streaks')
export class SparkStreak {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'varchar', unique: true })
  couple_key: string;

  @Column({ type: 'int', default: 0 })
  current_streak: number;

  @Column({ type: 'int', default: 0 })
  longest_streak: number;

  @Column({ type: 'int', default: 0 })
  total_sparks: number;

  @Column({ type: 'date', nullable: true })
  last_spark_date: string;

  @UpdateDateColumn()
  updated_at: Date;
}
