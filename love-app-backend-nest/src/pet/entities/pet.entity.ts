import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';

@Entity('pets')
export class Pet {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ unique: true })
  couple_key: string;

  @Column({ default: '' })
  name: string;

  @Column({ default: 'cat' })
  pet_type: string;

  @Column({ type: 'int', default: 1 })
  level: number;

  @Column({ type: 'int', default: 0 })
  xp: number;

  @Column({ type: 'int', default: 80 })
  hunger: number;

  @Column({ type: 'int', default: 80 })
  happiness: number;

  @Column({ type: 'int', default: 80 })
  cleanliness: number;

  @Column({ type: 'int', default: 100 })
  energy: number;

  @Column({ default: 'happy' })
  mood: string;

  @Column({ type: 'int', default: 100 })
  coins: number;

  @Column({ type: 'boolean', default: false })
  is_sleeping: boolean;

  @Column({ type: 'timestamp', nullable: true })
  last_fed: Date;

  @Column({ type: 'timestamp', nullable: true })
  last_played: Date;

  @Column({ type: 'timestamp', nullable: true })
  last_cleaned: Date;

  @Column({ type: 'timestamp', nullable: true })
  last_decay: Date;

  @Column({ type: 'int', default: 0 })
  streak_days: number;

  @Column({ type: 'int', default: 0 })
  checkin_streak: number;

  @Column({ type: 'int', default: 0 })
  total_checkins: number;

  @Column({ type: 'int', default: 0 })
  adventure_count: number;

  @Column({ type: 'date', nullable: true })
  last_checkin: string;

  @CreateDateColumn()
  created_at: Date;

  @UpdateDateColumn()
  updated_at: Date;
}
