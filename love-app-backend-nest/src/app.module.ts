import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ScheduleModule } from '@nestjs/schedule';
import { BullModule } from '@nestjs/bullmq';
import { ThrottlerModule } from '@nestjs/throttler';
import { WinstonModule } from 'nest-winston';

import { envValidationSchema } from './config/env.validation';
import { databaseConfig } from './config/database.config';
import { redisConfig } from './config/redis.config';
import { jwtConfig } from './config/jwt.config';
import { createWinstonConfig } from './config/logger.config';

// Core modules
import { RedisModule } from './redis/redis.module';
import { FirebaseModule } from './firebase/firebase.module';
import { SharedModule } from './shared/shared.module';

// Feature modules
import { AuthModule } from './auth/auth.module';
import { UsersModule } from './users/users.module';
import { RelationshipModule } from './relationship/relationship.module';
import { NotesModule } from './notes/notes.module';
import { WishesModule } from './wishes/wishes.module';
import { MoodsModule } from './moods/moods.module';
import { ActivitiesModule } from './activities/activities.module';
import { CyclesModule } from './cycles/cycles.module';
import { CalendarsModule } from './calendars/calendars.module';
import { ChatModule } from './chat/chat.module';
import { GalleryModule } from './gallery/gallery.module';
import { ArtModule } from './art/art.module';
import { PetModule } from './pet/pet.module';
import { GamesModule } from './games/games.module';
import { AchievementsModule } from './achievements/achievements.module';
import { LettersModule } from './letters/letters.module';
import { BucketlistModule } from './bucketlist/bucketlist.module';
import { LocationModule } from './location/location.module';
import { GeofencesModule } from './geofences/geofences.module';
import { PhoneStatusModule } from './phone-status/phone-status.module';
import { MemorialModule } from './memorial/memorial.module';
import { SparkModule } from './spark/spark.module';
import { TasksModule } from './tasks/tasks.module';
import { SleepModule } from './sleep/sleep.module';
import { LoveTouchModule } from './love-touch/love-touch.module';
import { MissYouModule } from './miss-you/miss-you.module';
import { AppLockModule } from './app-lock/app-lock.module';
import { QuestionsModule } from './questions/questions.module';
import { IntimacyModule } from './intimacy/intimacy.module';
import { MomentsModule } from './moments/moments.module';
import { PlacesModule } from './places/places.module';
import { ShopModule } from './shop/shop.module';
import { PremiumModule } from './premium/premium.module';
import { StoryModule } from './story/story.module';
import { TimModule } from './tim/tim.module';
import { WidgetModule } from './widget/widget.module';
import { GifModule } from './gif/gif.module';
import { ChatSettingsModule } from './chat-settings/chat-settings.module';
import { UploadModule } from './upload/upload.module';
import { AdminModule } from './admin/admin.module';
import { MetricsModule } from './metrics/metrics.module';
import { HealthModule } from './health/health.module';
import { GatewaysModule } from './gateways/gateways.module';
import { QueuesModule } from './queues/queues.module';
import { CronModule } from './cron/cron.module';

@Module({
  imports: [
    // Configuration
    ConfigModule.forRoot({
      isGlobal: true,
      load: [databaseConfig, redisConfig, jwtConfig],
      validationSchema: envValidationSchema,
    }),

    // Logging
    WinstonModule.forRootAsync({
      useFactory: () =>
        createWinstonConfig(
          process.env.LOGS_DIR || './logs',
          process.env.LOG_LEVEL || 'info',
        ),
    }),

    // Database
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        type: 'postgres',
        host: config.get('PGHOST'),
        port: config.get<number>('PGPORT', 5432),
        username: config.get('PGUSER'),
        password: config.get('PGPASSWORD'),
        database: config.get('PGDATABASE'),
        synchronize: false,
        logging: config.get('NODE_ENV') !== 'production',
        autoLoadEntities: true,
        migrationsRun: false,
        migrations: ['dist/migrations/*{.ts,.js}'],
        extra: {
          max: 20,
          idleTimeoutMillis: 30000,
          connectionTimeoutMillis: 2000,
        },
      }),
    }),

    // Rate limiting
    ThrottlerModule.forRoot([
      { name: 'short', ttl: 1000, limit: 10 },
      { name: 'medium', ttl: 60000, limit: 100 },
      { name: 'long', ttl: 900000, limit: 300 },
    ]),

    // Scheduling
    ScheduleModule.forRoot(),

    // Queues
    BullModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        connection: {
          url: config.get<string>('REDIS_URL'),
        },
      }),
    }),

    // Core
    RedisModule,
    FirebaseModule,
    SharedModule,

    // Features
    AuthModule,
    UsersModule,
    RelationshipModule,
    NotesModule,
    WishesModule,
    MoodsModule,
    ActivitiesModule,
    CyclesModule,
    CalendarsModule,
    ChatModule,
    GalleryModule,
    ArtModule,
    PetModule,
    GamesModule,
    AchievementsModule,
    LettersModule,
    BucketlistModule,
    LocationModule,
    GeofencesModule,
    PhoneStatusModule,
    MemorialModule,
    SparkModule,
    TasksModule,
    SleepModule,
    LoveTouchModule,
    MissYouModule,
    AppLockModule,
    QuestionsModule,
    IntimacyModule,
    MomentsModule,
    PlacesModule,
    ShopModule,
    PremiumModule,
    StoryModule,
    TimModule,
    WidgetModule,
    GifModule,
    ChatSettingsModule,
    UploadModule,
    AdminModule,
    MetricsModule,
    HealthModule,
    GatewaysModule,
    QueuesModule,
    CronModule,
  ],
})
export class AppModule {}
