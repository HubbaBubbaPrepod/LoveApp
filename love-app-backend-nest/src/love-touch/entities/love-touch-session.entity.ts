import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('love_touch_sessions')
export class LoveTouchSession {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'varchar' })
  couple_key: string;

  @Column({ type: 'bigint' })
  started_by: number;

  @Column({ type: 'boolean', default: false })
  partner_joined: boolean;

  @Column({ type: 'timestamp', nullable: true })
  ended_at: Date;

  @Column({ type: 'int', default: 0 })
  hearts_count: number;

  @CreateDateColumn()
  created_at: Date;
}
