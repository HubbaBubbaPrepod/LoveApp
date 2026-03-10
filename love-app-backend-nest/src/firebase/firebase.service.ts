import { Injectable, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as admin from 'firebase-admin';

@Injectable()
export class FirebaseService implements OnModuleInit {
  private app: admin.app.App;

  constructor(private readonly config: ConfigService) {}

  onModuleInit() {
    if (admin.apps.length) {
      this.app = admin.apps[0]!;
      return;
    }

    const serviceAccount = this.config.get<string>(
      'FIREBASE_SERVICE_ACCOUNT',
    );

    let credential: admin.credential.Credential;
    if (serviceAccount) {
      try {
        const parsed = JSON.parse(serviceAccount);
        credential = admin.credential.cert(parsed);
      } catch {
        // eslint-disable-next-line @typescript-eslint/no-require-imports
        const fs = require('fs');
        const content = JSON.parse(fs.readFileSync(serviceAccount, 'utf8'));
        credential = admin.credential.cert(content);
      }
    } else {
      credential = admin.credential.applicationDefault();
    }

    this.app = admin.initializeApp({ credential });
  }

  get messaging(): admin.messaging.Messaging {
    return this.app.messaging();
  }

  get auth(): admin.auth.Auth {
    return this.app.auth();
  }
}
