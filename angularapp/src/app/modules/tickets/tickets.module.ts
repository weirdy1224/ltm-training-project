import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from 'src/app/shared/shared/shared.module';
import { AuthGuard } from 'src/app/components/authguard.guard';
import { ROLES } from '../../constant';
import { SupportComponent } from '../../components/support/support.component';
import { MyTicketsComponent } from '../../components/my-tickets/my-tickets.component';
import { TicketDetailComponent } from '../../components/ticket-detail/ticket-detail.component';
import { AdminTicketsComponent } from '../../components/admin-tickets/admin-tickets.component';
import { AdminTicketDetailComponent } from '../../components/admin-ticket-detail/admin-ticket-detail.component';

const USER = { roles: [ROLES.CUSTOMER, ROLES.INSTRUCTOR] };
const ADMIN = { roles: [ROLES.ADMIN] };

const routes: Routes = [
  // customers and instructors
  { path: 'support', component: SupportComponent, canActivate: [AuthGuard], data: USER },
  { path: 'my-tickets', component: MyTicketsComponent, canActivate: [AuthGuard], data: USER },
  { path: 'my-tickets/:id', component: TicketDetailComponent, canActivate: [AuthGuard], data: USER },
  // admins
  { path: 'admin/tickets', component: AdminTicketsComponent, canActivate: [AuthGuard], data: ADMIN },
  { path: 'admin/tickets/user/:userId', component: AdminTicketsComponent, canActivate: [AuthGuard], data: ADMIN },
  { path: 'admin/tickets/:id', component: AdminTicketDetailComponent, canActivate: [AuthGuard], data: ADMIN }
];

/**
 * The support ticket system: help page and ticket pages for customers and instructors, ticket pages for admins.
 * Lazy loaded; each route is protected by AuthGuard with its own roles.
 */
@NgModule({
  declarations: [SupportComponent, MyTicketsComponent, TicketDetailComponent, AdminTicketsComponent, AdminTicketDetailComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class TicketsModule {}