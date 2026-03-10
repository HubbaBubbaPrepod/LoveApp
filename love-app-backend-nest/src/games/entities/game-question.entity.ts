import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('game_questions')
export class GameQuestion {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'varchar' })
  game_type: string;

  @Column({ type: 'varchar' })
  category: string;

  @Column({ type: 'text' })
  question_text: string;

  @Column({ type: 'text' })
  option_a: string;

  @Column({ type: 'text' })
  option_b: string;

  @Column({ type: 'text', nullable: true })
  option_c: string;

  @Column({ type: 'text', nullable: true })
  option_d: string;

  @Column({ type: 'varchar', nullable: true })
  correct_answer: string;

  @CreateDateColumn()
  created_at: Date;
}
