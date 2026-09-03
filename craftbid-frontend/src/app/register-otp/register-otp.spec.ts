import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegisterOtp } from './register-otp';

describe('RegisterOtp', () => {
  let component: RegisterOtp;
  let fixture: ComponentFixture<RegisterOtp>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterOtp],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterOtp);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
