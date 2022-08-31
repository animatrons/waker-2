import { FulfillmentMethod } from './FulfillmentMethod';
import { APenalty } from './penalties/APenalty';
import { User } from './User';

export class Reminder {
  key: string;
  user: User;
  name: string;
  description: string;
  notifyTime: Date;
  deadline: Date;
  fulfillment: FulfillmentMethod;
  penalty: APenalty;
  status: number;
  active: boolean;
}
