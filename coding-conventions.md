# Coding conventions that we follow throughout this codebase

- Prefer builders over writing multiple setters. E.g. instead var x = new MyObject; x.setFieldA(); x.setFieldB(). Use a builder - MyObject.builder().fieldA(A).fieldB(B) etc.
- Prefer lombok constructors, builders, getters, setters over writing your own.
- Prefer `var` over writing explicit types.
- If you have to get time as an argument always use Linux epoch timestamps