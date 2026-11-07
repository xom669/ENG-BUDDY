from __future__ import annotations

import asyncio
import logging
import uuid
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine
from sqlalchemy import select

from app.core.config import settings
from app.db.models import Base, Subject, User

logger = logging.getLogger("seed")
logging.basicConfig(level=logging.INFO)


async def seed_database() -> None:
    logger.info("Connecting to database: %s", settings.postgres_dsn)
    engine = create_async_engine(settings.postgres_dsn, echo=True)
    async_session = async_sessionmaker(engine, expire_on_commit=False)

    async with engine.begin() as conn:
        logger.info("Ensuring database tables exist...")
        await conn.run_sync(Base.metadata.create_all)

    async with async_session() as session:
        # Check if default user exists
        stmt = select(User).where(User.email == "aarav.sharma@iitb.ac.in")
        res = await session.execute(stmt)
        user = res.scalars().first()

        if not user:
            logger.info("Creating default student user: Aarav Sharma")
            user = User(
                id=uuid.uuid4(),
                email="aarav.sharma@iitb.ac.in",
                display_name="Aarav Sharma",
                tier="student_pro",
                daily_ai_quota_used=4,
                wifi_only_sync=True,
            )
            session.add(user)
            await session.commit()
            await session.refresh(user)

        # Check default subjects
        subj_stmt = select(Subject).where(Subject.user_id == user.id)
        existing_subjs = (await session.execute(subj_stmt)).scalars().all()

        if not existing_subjs:
            logger.info("Seeding engineering subjects for user %s", user.id)
            subjs = [
                Subject(
                    id=uuid.uuid4(),
                    user_id=user.id,
                    code="ME-204",
                    name="Thermodynamics & Heat Transfer",
                    target_attendance_pct=75.0,
                    lab_weight_classes=2,
                    count_medical_leave_as_excused=True,
                    rounding_mode="NEAREST",
                    attended_classes=25,
                    total_classes=32,
                    medical_excused_classes=1,
                ),
                Subject(
                    id=uuid.uuid4(),
                    user_id=user.id,
                    code="EC-304",
                    name="Signals & Systems",
                    target_attendance_pct=75.0,
                    lab_weight_classes=2,
                    count_medical_leave_as_excused=True,
                    rounding_mode="FLOOR",
                    attended_classes=18,
                    total_classes=28,
                    medical_excused_classes=0,
                ),
                Subject(
                    id=uuid.uuid4(),
                    user_id=user.id,
                    code="MA-302",
                    name="Engineering Mathematics III",
                    target_attendance_pct=75.0,
                    lab_weight_classes=1,
                    count_medical_leave_as_excused=False,
                    rounding_mode="NEAREST",
                    attended_classes=22,
                    total_classes=30,
                    medical_excused_classes=0,
                ),
            ]
            session.add_all(subjs)
            await session.commit()
            logger.info("Seeded 3 subjects successfully!")
        else:
            logger.info("Subjects already exist (%d subjects found).", len(existing_subjs))

    await engine.dispose()
    logger.info("Database seeding complete!")


if __name__ == "__main__":
    asyncio.run(seed_database())
