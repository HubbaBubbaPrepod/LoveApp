import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('custom_activity_types')
export class CustomActivityType {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'varchar' })
  name: string;

  @Column({ type: 'varchar', nullable: true })
  icon: string;

  @Column({ type: 'varchar', nullable: true })
  category: string;

  @CreateDateColumn()
  created_at: Date;
}
