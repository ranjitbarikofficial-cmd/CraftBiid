import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Artisan } from './artisan';

describe('Artisan', () => {
  let component: Artisan;
  let fixture: ComponentFixture<Artisan>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Artisan],
    }).compileComponents();

    fixture = TestBed.createComponent(Artisan);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
