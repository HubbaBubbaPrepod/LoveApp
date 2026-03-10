import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('game_rounds')
export class GameRound {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'int' })
  session_id: number;

  @Column({ type: 'int' })
  round_number: number;

  @Column({ type: 'int' })
  question_id: number;

  @Column({ type: 'varchar', nullable: true })
  user1_answer: string;

  @Column({ type: 'varchar', nullable: true })
  user2_answer: string;

  @Column({ type: 'boolean', nullable: true })
  is_match: boolean;

  @CreateDateColumn()
  created_at: Date;
}
