import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
} from 'typeorm';

@Entity('daily_questions')
export class DailyQuestion {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'text' })
  question_text: string;

  @Column({ type: 'varchar' })
  category: string;

  @Column({ type: 'text', nullable: true })
  options: string;
}
