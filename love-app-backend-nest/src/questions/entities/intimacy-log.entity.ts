import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('intimacy_logs')
export class IntimacyLog {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'varchar' })
  couple_key: string;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'varchar' })
  action_type: string;

  @Column({ type: 'int' })
  points: number;

  @CreateDateColumn()
  created_at: Date;
}
