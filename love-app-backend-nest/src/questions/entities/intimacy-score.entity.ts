import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  UpdateDateColumn,
} from 'typeorm';

@Entity('intimacy_scores')
export class IntimacyScore {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'varchar', unique: true })
  couple_key: string;

  @Column({ type: 'int', default: 0 })
  score: number;

  @Column({ type: 'int', default: 1 })
  level: number;

  @UpdateDateColumn()
  updated_at: Date;
}
