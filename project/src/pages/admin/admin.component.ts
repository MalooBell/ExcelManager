import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService, User, AuditLog } from '../../services/admin.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container py-8">
      <h1 class="text-3xl font-bold mb-6">Panneau d'Administration</h1>
      <div class="mb-4 border-b border-gray-200">
        <nav class="flex space-x-8">
          <button (click)="activeTab = 'users'" [class.active]="activeTab === 'users'" class="tab-btn">Utilisateurs</button>
          <button (click)="activeTab = 'audits'" [class.active]="activeTab === 'audits'" class="tab-btn">Journal d'Audit</button>
        </nav>
      </div>

      <div *ngIf="activeTab === 'users'" class="card">
        <h2 class="card-title mb-4">Utilisateurs Enregistrés</h2>
        <table class="table">
          <thead><tr><th>ID</th><th>Nom</th><th>Email</th><th>Rôle</th></tr></thead>
          <tbody><tr *ngFor="let user of users"><td>{{user.id}}</td><td>{{user.username}}</td><td>{{user.email}}</td><td><span class="badge" [class.badge-info]="user.role === 'ADMIN'">{{user.role}}</span></td></tr></tbody>
        </table>
      </div>

      <div *ngIf="activeTab === 'audits'" class="card">
        <h2 class="card-title mb-4">Historique des Actions</h2>
        <table class="table">
          <thead><tr><th>Date</th><th>Utilisateur</th><th>Action</th><th>Détails</th></tr></thead>
          <tbody><tr *ngFor="let audit of audits"><td>{{formatDate(audit.timestamp)}}</td><td>{{audit.username}}</td><td><span class="badge badge-warning">{{audit.action}}</span></td><td>{{audit.details}}</td></tr></tbody>
        </table>
      </div>
    </div>
  `,
})
export class AdminComponent implements OnInit {
  activeTab: 'users' | 'audits' = 'users';
  users: User[] = [];
  audits: AuditLog[] = [];

  constructor(private adminService: AdminService) { }

  ngOnInit(): void {
    this.adminService.getUsers().subscribe(data => this.users = data);
    this.adminService.getAuditLogs().subscribe(data => this.audits = data);
  }

  formatDate(d: string): string { return new Date(d).toLocaleString('fr-FR'); }
}