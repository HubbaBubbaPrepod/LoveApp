import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';

@Entity('game_sessions')
export class GameSession {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'varchar' })
  couple_key: string;

  @Column({ type: 'varchar' })
  game_type: string;

  @Column({ type: 'varchar', default: 'active' })
  status: string;

  @Column({ type: 'int', default: 5 })
  total_rounds: number;

  @Column({ type: 'int', default: 0 })
  current_round: number;

  @Column({ type: 'numeric', nullable: true })
  compatibility_score: number;

  @CreateDateColumn()
  created_at: Date;

  @UpdateDateColumn()
  updated_at: Date;
}
