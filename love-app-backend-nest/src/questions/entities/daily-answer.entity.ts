import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  Unique,
} from 'typeorm';

@Entity('daily_answers')
@Unique(['question_id', 'user_id', 'date'])
export class DailyAnswer {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'int' })
  question_id: number;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'varchar' })
  couple_key: string;

  @Column({ type: 'text' })
  answer: string;

  @Column({ type: 'date' })
  date: string;
}
