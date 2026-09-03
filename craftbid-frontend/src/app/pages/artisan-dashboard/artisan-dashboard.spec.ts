import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ArtisanDashboard } from './artisan-dashboard';

describe('ArtisanDashboard', () => {
  let component: ArtisanDashboard;
  let fixture: ComponentFixture<ArtisanDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ArtisanDashboard],
    }).compileComponents();

    fixture = TestBed.createComponent(ArtisanDashboard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
