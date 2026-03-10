import { Injectable } from '@nestjs/common';
import { InjectMetric } from '@willsoto/nestjs-prometheus';
import { Histogram, Gauge } from 'prom-client';

@Injectable()
export class MetricsService {
  constructor(
    @InjectMetric('http_request_duration_seconds')
    public readonly httpRequestDuration: Histogram<string>,
    @InjectMetric('active_socket_connections')
    public readonly activeSocketConnections: Gauge<string>,
    @InjectMetric('db_query_duration_seconds')
    public readonly dbQueryDuration: Histogram<string>,
  ) {}
}
