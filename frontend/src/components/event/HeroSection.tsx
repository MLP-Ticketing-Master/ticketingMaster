import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useEventList } from "@/hooks";
import { ChevronLeft, ChevronRight } from "lucide-react";
import desktopLck from "@/assets/background.png";
import desktopVal from "@/assets/VALbanner.png";
import desktopPubg from "@/assets/PUBGbanner.png";
import desktopWatch from "@/assets/WATCHbanner.png";
import mobileLck from "@/assets/LCKmobile.png";
import mobileVal from "@/assets/VALmobile.png";
import mobilePubg from "@/assets/PUBGmobile.png";
import mobileWatch from "@/assets/WATCHmobile.png";

const banners = [
  {image:desktopLck,
    mobileImage: mobileLck,
    sportType: "LOL",
   },
  {image:desktopVal,
    mobileImage: mobileVal,
    sportType: "VALORANT",
   },
  {image:desktopPubg,
    mobileImage: mobilePubg,
    sportType: "PUBG",
   },
  {image:desktopWatch,
    mobileImage: mobileWatch,
    sportType: "OVERWATCH",
   },
  ];


export function HeroSection() {
  const [current, setCurrent] = useState(0);


  const { data: events = [] } = useEventList();

  const bannersWithLink = banners.map((banner) => {
    const event = events.find(
      (event) => event.sportType === banner.sportType
    );

      return {
        ...banner,
        link: event ? `/events/${event.eventId}` : "/events",
      };
    });

  const prevSlide = () => {
    setCurrent((prev) =>
      prev === 0 ? banners.length - 1 : prev - 1
    );
  };

  const nextSlide = () => {
    setCurrent((prev) =>
      (prev + 1) % banners.length
    );
  };

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrent((prev) => (prev + 1) % banners.length);
    }, 10000); // 10초

    return () => clearInterval(timer);
  }, []);

  return (
    <section>
      <div className="relative group
            h-[320px] md:h-[560px]
            w-full 
            ">
        <div
            className="flex h-full transition-transform duration-700 ease-in-out" 
            style={{
              transform: `translateX(-${current * 100}%)`,
              display: 'flex'
            }}
          >
          {bannersWithLink.map((banner, index) => (
            <Link
              key={index}
              to={banner.link}
              className="relative h-full min-w-full block"
            >
          <div className="relative h-full min-w-full">
            <picture>
            <source
              media="(max-width: 768px)"
              srcSet={banner.mobileImage}
            />
            <img
              src={banner.image}
              alt={`banner-${index}`}
              className="h-[320px] md:h-[560px] w-full object-cover"
            />
            
            </picture>
            <div className="absolute inset-0 bg-black/20 hover:bg-black/10 transition pointer-events-none" />
            
          </div>
          </Link>
        ))}
      </div>


      {/* 하단 인디케이터 */}
      <div className="absolute bottom-6 left-1/2 z-20 flex -translate-x-1/2 gap-2">
        {banners.map((_, index) => (
          <button
            key={index}
            onClick={() => setCurrent(index)}
            className={`h-2 transition-all rounded-full ${
              current === index
                ? "w-8 bg-white"
                : "w-2 bg-white/50"
            }`}
          />
        ))}
      </div>

      <button
      onClick={prevSlide}
      className="
        absolute left-6 top-1/2 -translate-y-1/2 z-30
        h-24 w-24 rounded-full
        bg-white/10
        flex items-center justify-center
        text-white text-[40px]
        opacity-100
        group-hover:opacity-100
        hover:scale-110
        transition-all
      "
    >
       <ChevronLeft size={58} />
    </button>

    <button
      onClick={nextSlide}
      className="
        absolute right-6 top-1/2 -translate-y-1/2 z-30
        h-24 w-24 rounded-full
        bg-white/10
        flex items-center justify-center
        text-white text-[40px]
        opacity-100
        group-hover:opacity-100
        hover:scale-110
        transition-all
      "
    >
      <ChevronRight size={58} />
    </button>
    </div>
    </section>
  );
}