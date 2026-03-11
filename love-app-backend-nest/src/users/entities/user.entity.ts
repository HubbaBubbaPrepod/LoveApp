import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';

@Entity('users')
export class User {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column({ nullable: true })
  username: string;

  @Column({ nullable: true })
  email: string;

  @Column({ nullable: true, select: false })
  password_hash: string;

  @Column({ nullable: true })
  display_name: string;

  @Column({ nullable: true })
  profile_image: string;

  @Column({ nullable: true })
  avatar_url: string;

  @Column({ nullable: true })
  gender: string;

  @Column({ nullable: true })
  google_id: string;

  @Column({ default: 'user' })
  role: string;

  @Column({ default: false })
  is_premium: boolean;

  @Column({ type: 'decimal', default: 1.0 })
  premium_coins_multiplier: number;

  @Column({ nullable: true })
  pairing_code: string;

  @Column({ type: 'timestamptz', nullable: true })
  pairing_code_expires_at: Date;

  @CreateDateColumn()
  created_at: Date;

  @UpdateDateColumn()
  updated_at: Date;
}
