import { Address } from './Address';

export class User {
  key: string;
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  country: string;
  address: Address;
  birthDay: Date;
  phone: string;
}
