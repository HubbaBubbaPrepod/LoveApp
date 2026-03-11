import {
  Entity,
  PrimaryColumn,
  Column,
  UpdateDateColumn,
} from 'typeorm';

@Entity('fcm_tokens')
export class FcmToken {
  @PrimaryColumn({ type: 'int' })
  user_id: number;

  @Column({ type: 'text', nullable: true })
  fcm_token: string;

  @Column({ nullable: true })
  device_id: string;

  @UpdateDateColumn()
  updated_at: Date;
}
