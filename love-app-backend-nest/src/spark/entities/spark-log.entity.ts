import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  Unique,
} from 'typeorm';

@Entity('spark_logs')
@Unique(['couple_key', 'user_id', 'date'])
export class SparkLog {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'varchar' })
  couple_key: string;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'varchar' })
  spark_type: string;

  @Column({ type: 'date' })
  date: string;
}
